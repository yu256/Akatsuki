import com.github.tminglei.slickpg.*

trait MyPostgresDriver
    extends ExPostgresProfile
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport {
  override val api: MyAPI.type = MyAPI
  object MyAPI
      extends ExtPostgresAPI
      with ArrayImplicits
      with SimpleArrayPlainImplicits
      with Date2DateTimeImplicitsDuration
      with Date2DateTimePlainImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {}
}

object MyPostgresDriver extends MyPostgresDriver
