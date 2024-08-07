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
    given [A: izumi.reflect.Tag]: GetResult[List[A]] = mkGetResult(
      _.nextArray[A]().toList
    )
    given [A: izumi.reflect.Tag]: GetResult[Option[List[A]]] =
      mkGetResult(_.nextArrayOption[A]().map(_.toList))
    given GetResult[Option[InetString]] = mkGetResult(
      _.nextStringOption().map(InetString.apply)
    )
  }
}

object MyPostgresDriver extends MyPostgresDriver
