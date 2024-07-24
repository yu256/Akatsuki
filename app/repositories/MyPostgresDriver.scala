package repositories

import com.github.tminglei.slickpg.*
import com.github.tminglei.slickpg.agg.PgAggFuncSupport
import com.github.tminglei.slickpg.utils.PlainSQLUtils.mkGetResult
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.GetResult

trait MyPostgresDriver
    extends ExPostgresProfile
    with PgArraySupport
    with PgAggFuncSupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgPlayJsonSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport {
  def pgjson =
    "jsonb"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api: MyAPI.type = MyAPI

  object MyAPI
      extends ExtPostgresAPI
      with ArrayImplicits
      with SimpleArrayPlainImplicits
      with Date2DateTimeImplicitsDuration
      with Date2DateTimePlainImplicits
      with JsonImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] =
      new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper: DriverJdbcType[List[JsValue]] =
      new AdvancedArrayJdbcType[JsValue](
        pgjson,
        utils.SimpleArrayUtils.fromString[JsValue](Json.parse)(_).orNull,
        utils.SimpleArrayUtils.mkString[JsValue](_.toString)(_)
      ).to(_.toList)
    implicit val getLongList: GetResult[List[Long]] = mkGetResult(
      _.nextArray[Long]().toList
    )
    implicit val getInetStringOpt: GetResult[Option[InetString]] = mkGetResult(
      _.nextStringOption().map(InetString.apply)
    )
  }
}

object MyPostgresDriver extends MyPostgresDriver
