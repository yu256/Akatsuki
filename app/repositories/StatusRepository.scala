package repositories

import models.Status
import slick.dbio.DBIO
import slick.jdbc.GetResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait StatusRepository {
  enum TimelineType:
    case User(
        targetId: Long,
        userId: Option[Long] = None,
        onlyMedia: Boolean = false
    )
    case Home(id: Long)
    case Public(
        local: Boolean = false,
        remote: Boolean = false,
        onlyMedia: Boolean = false
    )

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
      limit: Int,
      sinceId: Option[Long] = None,
      maxId: Option[Long] = None
  ): DBIO[Seq[Status]]
}

@Singleton
class StatusRepositoryImpl @Inject() ()(using ExecutionContext)
    extends StatusRepository {
  import MyPostgresDriver.api.*

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
      limit: Int,
      sinceId: Option[Long] = None,
      maxId: Option[Long] = None
  ): DBIO[Seq[Status]] =
    import TimelineType.*
    val baseQuery = Tables.Statuses.filter(_.deletedAt.isEmpty)
    val partialQuery = tlType match {
      case User(targetId, userId, onlyMedia) =>
        val isFollowing =
          Tables.Follows
            .filter(f =>
              f.accountId === userId && f.targetAccountId === targetId
            )
            .exists || userId.fold(false)(_ == targetId)

        val statuses = baseQuery.filter(s =>
          s.accountId === targetId && {
            Case If isFollowing Then s.visibility =!= 3 Else s.visibility === 0
          }
        )

        if onlyMedia then baseQuery.filter(_.mediaAttachmentIds.length() =!= 0)
        else baseQuery

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
      case Public(local, remote, onlyMedia) =>
        val predicates = Seq.newBuilder[Tables.Statuses => Rep[Boolean]]
        if local then predicates += (_.local)
        if remote then predicates += (!_.local)
        if onlyMedia then predicates += (_.mediaAttachmentIds.length() =!= 0)
        predicates.result
          .foldLeft(baseQuery.filter(_.visibility === 0))(_.filter(_))
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
        .take(limit)
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
