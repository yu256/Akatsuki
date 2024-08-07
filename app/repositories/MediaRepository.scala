package repositories

import models.MediaAttachment
import slick.dbio.DBIO

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait MediaRepository {
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
  ): DBIO[MediaAttachment]
}

@Singleton
class MediaRepositoryImpl @Inject() ()(using ExecutionContext)
    extends MediaRepository {
  import MyPostgresDriver.api.given

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
  ): DBIO[MediaAttachment] =
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
