import slick.codegen.SourceCodeGenerator
import slick.sql.SqlProfile.ColumnOption

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object CustomizedCodeGenerator {

  import Config.*

  private val projectDir: String = Paths
    .get("")
    .toAbsolutePath
    .toString
    .split("""\\""")
    .dropRight(1)
    .mkString("""/""")

  implicit val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    Await.result(
      codegen.map(
        _.writeToFile(
          "MyPostgresDriver",
          s"$projectDir/app/repositories",
          "",
          "Tables",
          "Tables.scala"
        )
      ),
      20.seconds
    )
  }

  private val db: slickProfile.backend.JdbcDatabaseDef =
    slickProfile.api.Database.forURL(url, driver = jdbcDriver)

  private lazy val codegen: Future[SourceCodeGenerator] = db
    .run {
      slickProfile.defaultTables
        .map(_.filterNot(_.name.name == "play_evolutions"))
        .flatMap(
          slickProfile
            .createModelBuilder(_, ignoreInvalidDefaults = true)
            .buildModel
        )
    }
    .map { model =>
      new slick.codegen.SourceCodeGenerator(model) {
        override def Table = new Table(_) {
          table =>
          override def Column = new Column(_) {
            column =>
            override def rawType: String = {
              this.model.options
                .find(_.isInstanceOf[ColumnOption.SqlType])
                .flatMap {
                  _.asInstanceOf[ColumnOption.SqlType].typeName match {
                    case "hstore" => Some("Map[String, String]")
                    case "_text" | "text[]" | "_varchar" | "varchar[]" =>
                      Some("List[String]")
                    case "_int8" | "int8[]" => Some("List[Long]")
                    case "_int4" | "int4[]" => Some("List[Int]")
                    case "_int2" | "int2[]" => Some("List[Short]")
                    case _                  => None
                  }
                }
                .getOrElse {
                  this.model.tpe match {
                    case "java.sql.Timestamp" => "java.time.ZonedDateTime"
                    case _ =>
                      super.rawType
                  }
                }
            }
          }
        }

        override def packageCode(
            profile: String,
            pkg: String,
            container: String,
            parentType: Option[String]
        ): String = {
          s"""
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
package repositories

object ${container} extends ${container}(${profile})

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${container}(val profile: ${profile})${parentType
              .map(t => s" extends $t")
              .getOrElse("")} {
  import profile.api.*
  ${indent(code)}
}
      """.trim.replaceAll("""\((\w+\.apply) _\)""", "$1")
        }
      }
    }
}
