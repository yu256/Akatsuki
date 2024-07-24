package repositories

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait FollowRepository {}

@Singleton
class FollowRepositoryImpl @Inject (dbConfigProvider: DatabaseConfigProvider)(
    using ExecutionContext
) extends FollowRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

}