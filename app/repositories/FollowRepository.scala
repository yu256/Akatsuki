package repositories

import slick.dbio.DBIO

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait FollowRepository {
  def follow(
      accountId: Long,
      targetAccountId: Long,
      showReblogs: Boolean = true,
      inform: Boolean = false,
      languages: Seq[String] = Seq.empty
  ): DBIO[Tables.FollowsRow]

  def getInfo(
      accountId: Long,
      targetAccountId: Long
  ): DBIO[Option[Tables.FollowsRow]]

  def unfollow(accountId: Long, targetAccountId: Long): DBIO[Int]
}

@Singleton
class FollowRepositoryImpl @Inject() ()(using ExecutionContext)
    extends FollowRepository {
  import MyPostgresDriver.api.given

  def follow(
      accountId: Long,
      targetAccountId: Long,
      showReblogs: Boolean,
      inform: Boolean,
      languages: Seq[String]
  ): DBIO[Tables.FollowsRow] =
    sql"""
         INSERT INTO follows (account_id, target_account_id, show_reblogs, inform, languages)
         VALUES ($accountId, $targetAccountId, $showReblogs, $inform, $languages)
         ON CONFLICT (account_id, target_account_id) DO UPDATE
         SET show_reblogs = $showReblogs, inform = $inform, languages = $languages, updated_at = clock_timestamp()
         WHERE follows.account_id = $accountId AND follows.target_account_id = $targetAccountId
         RETURNING *
       """.as[Tables.FollowsRow].head

  def getInfo(
      accountId: Long,
      targetAccountId: Long
  ): DBIO[Option[Tables.FollowsRow]] =
    Tables.Follows
      .filter(follow =>
        follow.accountId === accountId && follow.targetAccountId === targetAccountId
      )
      .result
      .headOption

  def unfollow(accountId: Long, targetAccountId: Long): DBIO[Int] =
    Tables.Follows
      .filter(follow =>
        follow.accountId === accountId && follow.targetAccountId === targetAccountId
      )
      .delete
}
