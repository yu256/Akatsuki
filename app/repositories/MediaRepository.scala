package repositories

import extensions.DBIOA
import models.MediaAttachment
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait MediaRepository extends Repository {
  def create(
      fileName: String,
      contentType: Option[String],
      fileSize: Long,
      accountId: Long,
      blurhash: String,
      thumbnailFileName: String,
      thumbnailContentType: Option[String],
      thumbnailFileSize: Long,
      url: Option[String] = None,
      thumbnailUrl: Option[String] = None,
      remoteUrl: Option[String] = None
  ): DBIOA[MediaAttachment]
}

@Singleton
class MediaRepositoryImpl @Inject (dbConfigProvider: DatabaseConfigProvider)(
    using ExecutionContext
) extends MediaRepository {
  val dbConfig = dbConfigProvider.get[PostgresProfile]

  import MyPostgresDriver.api.*
  
  def run[T] = dbConfig.db.run[T]

  def create(
      fileName: String,
      contentType: Option[String],
      fileSize: Long,
      accountId: Long,
      blurhash: String,
      thumbnailFileName: String,
      thumbnailContentType: Option[String],
      thumbnailFileSize: Long,
      url: Option[String] = None,
      thumbnailUrl: Option[String] = None,
      remoteUrl: Option[String] = None
  ): DBIOA[MediaAttachment] =
    sql"""
       INSERT INTO media (
          file_name, content_type, file_size, account_id, blurhash, thumbnail_file_name, thumbnail_content_type, thumbnail_file_size, url, thumbnail_url, remote_url
       ) VALUES (
          $fileName, $contentType, $fileSize, $accountId, $blurhash, $thumbnailFileName, $thumbnailContentType, $thumbnailFileSize, $url, $thumbnailUrl, $remoteUrl
       ) RETURNING *
    """
      .as[Tables.MediaRow]
      .head
      .map(MediaAttachment.fromRow)

}
