package repositories

import cats.data.OptionT
import cats.syntax.all.*
import models.Status
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.{GetResult, PostgresProfile}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StatusRepository extends Repository {
  enum TimelineType:
    case User(id: Long, showDM: Boolean = false)
    case Home(id: Long)
    case Local
    case Global

  def createStatus(
      accountId: Long,
      text: String,
      sensitive: Boolean = false,
      spoilerText: Option[String] = None,
      visibility: Int | String = 0,
      language: Option[String] = None,
      mediaIds: Seq[Long] = Seq.empty
  ): DBIO[Status]
  def deleteStatus(statusId: Long, userId: Long): DBIO[Option[Status]]
  def timeline(
      tlType: TimelineType,
      max: Int,
      sinceId: Option[Long] = None,
      maxId: Option[Long] = None
  ): DBIO[Seq[Status]]
}

@Singleton
class StatusRepositoryImpl @Inject() (
    dbConfigProvider: DatabaseConfigProvider
)(using ExecutionContext)
    extends StatusRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  import dbConfig.*

  def run[T] = db.run[T]

  given mediaSeqGR(using
      mediaGR: GetResult[Tables.MediaRow]
  ): GetResult[Seq[Tables.MediaRow]] =
    GetResult { r =>
      Iterator
        .continually(r.rs)
        .takeWhile(_.next())
        .map { _ =>
          mediaGR(r)
        }
        .toSeq
    }

  def createStatus(
      accountId: Long,
      text: String,
      sensitive: Boolean = false,
      spoilerText: Option[String] = None,
      visibility: Int | String = 0,
      language: Option[String] = None,
      mediaIds: Seq[Long] = Seq.empty
  ): DBIO[Status] = {
    val visibilityValue = visibility match {
      case 0 | "public"   => 0
      case 1 | "unlisted" => 1
      case 2 | "private"  => 2
      case 3 | "direct"   => 3
      case _ => throw IllegalArgumentException("Invalid visibility value")
    }

    sql"""
      WITH inserted AS (
        INSERT INTO statuses (
            text, sensitive, visibility, spoiler_text, language, account_id, media_attachment_ids
        ) VALUES (
            $text, $sensitive, $visibilityValue, ${spoilerText.getOrElse(
        ""
      )}, $language, $accountId, $mediaIds
        ) RETURNING *
      )
      SELECT inserted.*, accounts.*, media_files.media_files
      FROM inserted
      LEFT JOIN accounts ON inserted.account_id = accounts.id
      LEFT JOIN LATERAL (
        SELECT array_agg(row_to_json(media)) as media_files
        FROM media
        WHERE media.id = ANY(inserted.media_attachment_ids)
      ) media_files ON true;
    """
      .as[(Tables.StatusesRow, Tables.AccountsRow, Seq[Tables.MediaRow])]
      .head
      .map(Status.fromRow)
  }

  def deleteStatus(statusId: Long, userId: Long): DBIO[Option[Status]] =
    sql"""
             WITH deleted AS (
               UPDATE statuses
               SET deleted_at = clock_timestamp()
               WHERE id = $statusId AND account_id = $userId
               RETURNING *
             )
             SELECT deleted.*, accounts.*, media_files.media_files
             FROM deleted
             LEFT JOIN accounts ON deleted.account_id = accounts.id
             LEFT JOIN LATERAL (
               SELECT array_agg(row_to_json(media)) as media_files
               FROM media
               WHERE media.id = ANY(deleted.media_attachment_ids)
             ) media_files ON true;
           """
      .as[(Tables.StatusesRow, Tables.AccountsRow, Seq[Tables.MediaRow])]
      .headOption
      .map(_.map(Status.fromRow))

  def timeline(
      tlType: TimelineType,
      max: Int,
      sinceId: Option[Long] = None,
      maxId: Option[Long] = None
  ): DBIO[Seq[Status]] =
    import TimelineType.*
    val baseQuery = Tables.Statuses.filter(_.deletedAt.isEmpty)
    val partialQuery = tlType match {
      case User(id, showDM) =>
        val withDM = baseQuery.filter(_.accountId === id)
        if (showDM) withDM else withDM.filterNot(_.visibility === 3)
      case Home(id) =>
        val followTargetIds = Tables.Follows
          .filter(_.accountId === id)
          .map(_.targetAccountId)
        baseQuery
          .filter(s =>
            s.accountId === id || (s.visibility =!= 3 && s.accountId.in(
              followTargetIds
            ))
          )
      case Local  => baseQuery.filter(s => s.visibility === 0 && s.local)
      case Global => baseQuery.filter(_.visibility === 0)
    }

    val paginatedQuery = (sinceId, maxId) match {
      case (Some(since), None) => partialQuery.filter(_.id > since)
      case (None, Some(max))   => partialQuery.filter(_.id < max)
      case (Some(since), Some(max)) =>
        partialQuery.filter(s => s.id > since && s.id < max)
      case _ => partialQuery
    }

    (for {
      status <- paginatedQuery
        .take(max)
        .sortBy(_.createdAt.desc)
      account <- Tables.Accounts if account.id === status.accountId
    } yield (status, account)).result
      .flatMap(seq =>
        DBIO.sequence(seq.map { (status, account) =>
          Tables.Media
            .filter(_.id.inSet(status.mediaAttachmentIds))
            .result
            .map {
              Status.fromRow(status, account, _)
            }
        })
      )
}
