package controllers.api.v2

import cats.syntax.all.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.*
import play.api.mvc.*
import play.api.{Configuration, Environment}
import repositories.{AuthRepository, MediaRepository}
import security.{AuthController, UserRequest}

import java.io.File
import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{Files, Paths}
import javax.imageio.ImageIO
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import org.im4java.core.{IMOperation, ImageMagickCmd}
import org.im4java.process.StandardStream

import java.util.concurrent.Executors

class MediaController @Inject() (
    authRepo: AuthRepository,
    cc: ControllerComponents,
    dbConfigProvider: DatabaseConfigProvider,
    env: Environment,
    config: Configuration,
    mediaRepo: MediaRepository,
    ec: ExecutionContext
) extends AuthController(authRepo, cc, dbConfigProvider)(ec) {
  import MediaController.executionContext
  def serveFile(fileName: String): Action[AnyContent] = Action.async {
    request =>
      val file = new File(env.getFile("media"), fileName)
      Future {
        if file.exists && file.isFile then
          Ok.sendFile(
            content = file,
            inline = true,
            fileName = _ => Some(URLDecoder.decode(fileName, "UTF-8"))
          )
        else NotFound("File not found")
      }
  }

  val post: Action[MultipartFormData[TemporaryFile]] =
    authAction(parse.multipartFormData).async {
      case UserRequest(user, request) =>
        request.body
          .file("file")
          .fold(BadRequest("Invalid file").pure[Future]) { fileData =>
            // todo: 同じファイル名のとき上書きされるのを直す
            val filename = URLEncoder.encode(fileData.filename, "UTF-8")
            val dirPath = s"${System.getProperty("user.dir")}/media"

            val fileSize = fileData.fileSize

            val file = File(s"$dirPath/$filename")
            val thumbnailFile = File(s"$dirPath/thumbnail_$filename")

            val baseUrl = s"${config.get[String]("app.url")}/media"

            for {
              blurhash <- Future {
                Files.createDirectories(Paths.get(dirPath))
                fileData.ref.moveTo(file, replace = true)
                compressImage(file.getPath, thumbnailFile.getPath)
                BlurHashEncoder.encodeBlurHash(
                  8,
                  8,
                  ImageIO.read(thumbnailFile)
                )
              }
              media <- run(
                mediaRepo.create(
                  fileName = filename,
                  contentType = fileData.contentType,
                  fileSize = fileSize,
                  accountId = user.accountId,
                  blurhash = blurhash,
                  thumbnailFileName = filename,
                  thumbnailContentType = fileData.contentType,
                  thumbnailFileSize = fileSize,
                  url = s"$baseUrl/$filename".some,
                  thumbnailUrl = s"$baseUrl/thumbnail_$filename".some
                )
              )
            } yield {
              Ok(Json.toJson(media))
            }
          }
    }

  private def compressImage(input: String, output: String): Unit = {
    val cmd = ImageMagickCmd("magick")

    cmd.setErrorConsumer(StandardStream.STDERR)

    val op = IMOperation()
    op.addImage(input)
    op.thumbnail(200, 200)
    op.quality(75.0)
    op.addImage(output)

    cmd.run(op)
  }

  private object BlurHashEncoder {
    import java.awt.Color
    import java.awt.image.BufferedImage
    import scala.math.*

    def encodeBlurHash(
        xComp: Int,
        yComp: Int,
        image: BufferedImage
    ): String = {
      val width = image.getWidth
      val height = image.getHeight
      val dc :: ac =
        (for (y <- (0 until yComp).toList; x <- 0 until xComp) yield {
          calculateBasis(image, width, height, x, y)
        }): @unchecked

      val sizeFlag = (xComp - 1) + (yComp - 1) * 9

      val (maximumValue, quantisedMaximumValue) =
        ac.map { case (r, g, b) =>
          max(max(abs(r), abs(g)), abs(b))
        }.maxOption match {
          case Some(actualMaximumValue) =>
            val quantisedMaximumValue =
              max(0, min(82, floor(actualMaximumValue * 166 - 0.5))).toInt
            val maximumValue = (quantisedMaximumValue + 1).toFloat / 166
            maximumValue -> quantisedMaximumValue
          case None => 1f -> 0
        }

      sizeFlag.encode83(1)
        ++ quantisedMaximumValue.encode83(1)
        ++ encodeDC(dc).encode83(4)
        ++ ac.map(encodeAC(_, maximumValue).encode83(2)).mkString
    }

    private def calculateBasis(
        image: BufferedImage,
        width: Int,
        height: Int,
        xIdx: Int,
        yIdx: Int
    ): (Float, Float, Float) = {
      val normalisation = if xIdx == 0 && yIdx == 0 then 1d else 2d
      val scale = normalisation / (width * height)

      (for (x <- 0 until width; y <- 0 until height) yield {
        val basisMultipliedByScale =
          (cos(Pi * xIdx * x / width) * cos(
            Pi * yIdx * y / height
          ) * scale).toFloat
        val color = Color(image.getRGB(x, y))
        (
          basisMultipliedByScale * sRGBToLinear(color.getRed),
          basisMultipliedByScale * sRGBToLinear(color.getGreen),
          basisMultipliedByScale * sRGBToLinear(color.getBlue)
        )
      }).reduce { case ((r_, g_, b_), (r, g, b)) =>
        (r_ + r, g_ + g, b_ + b)
      }
    }

    private inline def encodeDC(rgb: (Float, Float, Float)): Int =
      (linearTosRGB(rgb._1) << 16) + (linearTosRGB(rgb._2) << 8) + linearTosRGB(
        rgb._3
      )

    private def encodeAC(
        rgb: (Float, Float, Float),
        maximumValue: Float
    ): Int = {
      inline def quantise(value: Float): Int =
        max(
          0,
          min(18, floor(signPow(value / maximumValue, 0.5f) * 9 + 9.5d).toInt)
        )
      quantise(rgb._1) * 19 * 19 + quantise(rgb._2) * 19 + quantise(rgb._3)
    }

    private def signPow(value: Float, exp: Float): Float =
      math.signum(value) * math.pow(math.abs(value), exp).toFloat

    private def linearTosRGB(value: Float): Int = {
      val v = max(0, min(1, value))
      if (v <= 0.0031308f) (v * 12.92f * 255 + 0.5f).toInt
      else ((1.055f * math.pow(v, 1 / 2.4) - 0.055f) * 255 + 0.5f).toInt
    }

    private def sRGBToLinear(value: Int): Float = {
      val v = value.toFloat / 255
      if (v <= 0.04045f) v / 12.92f
      else math.pow((v + 0.055f) / 1.055f, 2.4f).toFloat
    }

    private inline val encodeCharacters =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"

    extension (value: Int) {
      private def encode83(length: Int): String =
        (1 to length).foldLeft("") { (acc, i) =>
          acc + encodeCharacters.charAt {
            (value / math.pow(83, length - i).toInt) % 83
          }
        }
    }
  }
}

object MediaController {
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        Runtime.getRuntime.availableProcessors() / 2
      )
    )
}
