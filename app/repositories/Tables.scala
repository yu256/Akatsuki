package repositories

object Tables extends Tables(MyPostgresDriver)

trait Tables(val profile: MyPostgresDriver) {
  import profile.api.*
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for
  // tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(AccessTokens.schema, Accounts.schema, Applications.schema, FollowRequests.schema, Follows.schema, Media.schema, Statuses.schema, Users.schema).reduceLeft(_ ++ _)

  /** Entity class storing rows of table AccessTokens
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param token Database column token SqlType(text)
   *  @param refreshToken Database column refresh_token SqlType(text), Default(None)
   *  @param expiresIn Database column expires_in SqlType(int4), Default(None)
   *  @param revokedAt Database column revoked_at SqlType(timestamptz), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param scopes Database column scopes SqlType(text), Default(None)
   *  @param applicationId Database column application_id SqlType(int8), Default(None)
   *  @param resourceOwnerId Database column resource_owner_id SqlType(int8)
   *  @param lastUsedAt Database column last_used_at SqlType(timestamptz), Default(None)
   *  @param lastUsedIp Database column last_used_ip SqlType(inet), Default(None) */
  case class AccessTokensRow(id: Long, token: String, refreshToken: Option[String] = None, expiresIn: Option[Int] = None, revokedAt: Option[java.time.ZonedDateTime] = None, createdAt: java.time.ZonedDateTime, scopes: Option[String] = None, applicationId: Option[Long] = None, resourceOwnerId: Long, lastUsedAt: Option[java.time.ZonedDateTime] = None, lastUsedIp: Option[com.github.tminglei.slickpg.InetString] = None)
  /** GetResult implicit for fetching AccessTokensRow objects using plain SQL queries */
  implicit def GetResultAccessTokensRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[Int]], e4: GR[Option[java.time.ZonedDateTime]], e5: GR[java.time.ZonedDateTime], e6: GR[Option[Long]], e7: GR[Option[com.github.tminglei.slickpg.InetString]]): GR[AccessTokensRow] = GR{
    prs => import prs._
    AccessTokensRow.apply.tupled((<<[Long], <<[String], <<?[String], <<?[Int], <<?[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<?[String], <<?[Long], <<[Long], <<?[java.time.ZonedDateTime], <<?[com.github.tminglei.slickpg.InetString]))
  }
  /** Table description of table access_tokens. Objects of this class serve as prototypes for rows in queries. */
  class AccessTokens(_tableTag: Tag) extends profile.api.Table[AccessTokensRow](_tableTag, "access_tokens") {
    def * = ((id, token, refreshToken, expiresIn, revokedAt, createdAt, scopes, applicationId, resourceOwnerId, lastUsedAt, lastUsedIp)).mapTo[AccessTokensRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(token), refreshToken, expiresIn, revokedAt, Rep.Some(createdAt), scopes, applicationId, Rep.Some(resourceOwnerId), lastUsedAt, lastUsedIp)).shaped.<>({r=>import r._; _1.map(_=> AccessTokensRow.apply.tupled((_1.get, _2.get, _3, _4, _5, _6.get, _7, _8, _9.get, _10, _11)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column token SqlType(text) */
    val token: Rep[String] = column[String]("token")
    /** Database column refresh_token SqlType(text), Default(None) */
    val refreshToken: Rep[Option[String]] = column[Option[String]]("refresh_token", O.Default(None))
    /** Database column expires_in SqlType(int4), Default(None) */
    val expiresIn: Rep[Option[Int]] = column[Option[Int]]("expires_in", O.Default(None))
    /** Database column revoked_at SqlType(timestamptz), Default(None) */
    val revokedAt: Rep[Option[java.time.ZonedDateTime]] = column[Option[java.time.ZonedDateTime]]("revoked_at", O.Default(None))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column scopes SqlType(text), Default(None) */
    val scopes: Rep[Option[String]] = column[Option[String]]("scopes", O.Default(None))
    /** Database column application_id SqlType(int8), Default(None) */
    val applicationId: Rep[Option[Long]] = column[Option[Long]]("application_id", O.Default(None))
    /** Database column resource_owner_id SqlType(int8) */
    val resourceOwnerId: Rep[Long] = column[Long]("resource_owner_id")
    /** Database column last_used_at SqlType(timestamptz), Default(None) */
    val lastUsedAt: Rep[Option[java.time.ZonedDateTime]] = column[Option[java.time.ZonedDateTime]]("last_used_at", O.Default(None))
    /** Database column last_used_ip SqlType(inet), Default(None) */
    val lastUsedIp: Rep[Option[com.github.tminglei.slickpg.InetString]] = column[Option[com.github.tminglei.slickpg.InetString]]("last_used_ip", O.Default(None))

    /** Index over (resourceOwnerId) (database name idx_access_tokens_resource_owner_id) */
    val index1 = index("idx_access_tokens_resource_owner_id", resourceOwnerId)
    /** Uniqueness Index over (token) (database name index_access_tokens_token) */
    val index2 = index("index_access_tokens_token", token, unique=true)
  }
  /** Collection-like TableQuery object for table AccessTokens */
  lazy val AccessTokens = new TableQuery(tag => new AccessTokens(tag))

  /** Entity class storing rows of table Accounts
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param username Database column username SqlType(text), Default()
   *  @param domain Database column domain SqlType(text), Default(None)
   *  @param displayName Database column display_name SqlType(text), Default()
   *  @param locked Database column locked SqlType(bool), Default(false)
   *  @param bot Database column bot SqlType(bool), Default(false)
   *  @param note Database column note SqlType(text), Default()
   *  @param url Database column url SqlType(text), Default(None)
   *  @param fields Database column fields SqlType(jsonb), Length(2147483647,false), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz) */
  case class AccountsRow(id: Long, username: String = "", domain: Option[String] = None, displayName: String = "", locked: Boolean = false, bot: Boolean = false, note: String = "", url: Option[String] = None, fields: Option[String] = None, createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime)
  /** GetResult implicit for fetching AccountsRow objects using plain SQL queries */
  implicit def GetResultAccountsRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Boolean], e4: GR[java.time.ZonedDateTime]): GR[AccountsRow] = GR{
    prs => import prs._
    AccountsRow.apply.tupled((<<[Long], <<[String], <<?[String], <<[String], <<[Boolean], <<[Boolean], <<[String], <<?[String], <<?[String], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime]))
  }
  /** Table description of table accounts. Objects of this class serve as prototypes for rows in queries. */
  class Accounts(_tableTag: Tag) extends profile.api.Table[AccountsRow](_tableTag, "accounts") {
    def * = ((id, username, domain, displayName, locked, bot, note, url, fields, createdAt, updatedAt)).mapTo[AccountsRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(username), domain, Rep.Some(displayName), Rep.Some(locked), Rep.Some(bot), Rep.Some(note), url, fields, Rep.Some(createdAt), Rep.Some(updatedAt))).shaped.<>({r=>import r._; _1.map(_=> AccountsRow.apply.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11.get)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column username SqlType(text), Default() */
    val username: Rep[String] = column[String]("username", O.Default(""))
    /** Database column domain SqlType(text), Default(None) */
    val domain: Rep[Option[String]] = column[Option[String]]("domain", O.Default(None))
    /** Database column display_name SqlType(text), Default() */
    val displayName: Rep[String] = column[String]("display_name", O.Default(""))
    /** Database column locked SqlType(bool), Default(false) */
    val locked: Rep[Boolean] = column[Boolean]("locked", O.Default(false))
    /** Database column bot SqlType(bool), Default(false) */
    val bot: Rep[Boolean] = column[Boolean]("bot", O.Default(false))
    /** Database column note SqlType(text), Default() */
    val note: Rep[String] = column[String]("note", O.Default(""))
    /** Database column url SqlType(text), Default(None) */
    val url: Rep[Option[String]] = column[Option[String]]("url", O.Default(None))
    /** Database column fields SqlType(jsonb), Length(2147483647,false), Default(None) */
    val fields: Rep[Option[String]] = column[Option[String]]("fields", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")

    /** Uniqueness Index over (username,domain) (database name accounts_username_domain_key) */
    val index1 = index("accounts_username_domain_key", (username, domain), unique=true)
    /** Index over (createdAt) (database name idx_accounts_created_at) */
    val index2 = index("idx_accounts_created_at", createdAt)
    /** Index over (domain) (database name idx_accounts_domain) */
    val index3 = index("idx_accounts_domain", domain)
    /** Index over (updatedAt) (database name idx_accounts_updated_at) */
    val index4 = index("idx_accounts_updated_at", updatedAt)
    /** Index over (username) (database name idx_accounts_username) */
    val index5 = index("idx_accounts_username", username)
    /** Uniqueness Index over (username) (database name unique_username_null_domain) */
    val index6 = index("unique_username_null_domain", username, unique=true)
  }
  /** Collection-like TableQuery object for table Accounts */
  lazy val Accounts = new TableQuery(tag => new Accounts(tag))

  /** Entity class storing rows of table Applications
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(text)
   *  @param secret Database column secret SqlType(text)
   *  @param redirectUri Database column redirect_uri SqlType(text)
   *  @param scopes Database column scopes SqlType(text), Default()
   *  @param code Database column code SqlType(text), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz)
   *  @param website Database column website SqlType(text), Default(None)
   *  @param ownerType Database column owner_type SqlType(text), Default(None)
   *  @param ownerId Database column owner_id SqlType(int8), Default(None)
   *  @param confidential Database column confidential SqlType(bool), Default(true) */
  case class ApplicationsRow(id: Long, name: String, secret: String, redirectUri: String, scopes: String = "", code: Option[String] = None, createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime, website: Option[String] = None, ownerType: Option[String] = None, ownerId: Option[Long] = None, confidential: Boolean = true)
  /** GetResult implicit for fetching ApplicationsRow objects using plain SQL queries */
  implicit def GetResultApplicationsRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[java.time.ZonedDateTime], e4: GR[Option[Long]], e5: GR[Boolean]): GR[ApplicationsRow] = GR{
    prs => import prs._
    ApplicationsRow.apply.tupled((<<[Long], <<[String], <<[String], <<[String], <<[String], <<?[String], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<?[String], <<?[String], <<?[Long], <<[Boolean]))
  }
  /** Table description of table applications. Objects of this class serve as prototypes for rows in queries. */
  class Applications(_tableTag: Tag) extends profile.api.Table[ApplicationsRow](_tableTag, "applications") {
    def * = ((id, name, secret, redirectUri, scopes, code, createdAt, updatedAt, website, ownerType, ownerId, confidential)).mapTo[ApplicationsRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(secret), Rep.Some(redirectUri), Rep.Some(scopes), code, Rep.Some(createdAt), Rep.Some(updatedAt), website, ownerType, ownerId, Rep.Some(confidential))).shaped.<>({r=>import r._; _1.map(_=> ApplicationsRow.apply.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9, _10, _11, _12.get)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column secret SqlType(text) */
    val secret: Rep[String] = column[String]("secret")
    /** Database column redirect_uri SqlType(text) */
    val redirectUri: Rep[String] = column[String]("redirect_uri")
    /** Database column scopes SqlType(text), Default() */
    val scopes: Rep[String] = column[String]("scopes", O.Default(""))
    /** Database column code SqlType(text), Default(None) */
    val code: Rep[Option[String]] = column[Option[String]]("code", O.Default(None))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")
    /** Database column website SqlType(text), Default(None) */
    val website: Rep[Option[String]] = column[Option[String]]("website", O.Default(None))
    /** Database column owner_type SqlType(text), Default(None) */
    val ownerType: Rep[Option[String]] = column[Option[String]]("owner_type", O.Default(None))
    /** Database column owner_id SqlType(int8), Default(None) */
    val ownerId: Rep[Option[Long]] = column[Option[Long]]("owner_id", O.Default(None))
    /** Database column confidential SqlType(bool), Default(true) */
    val confidential: Rep[Boolean] = column[Boolean]("confidential", O.Default(true))

    /** Index over (ownerId,ownerType) (database name idx_applications_owner_id_and_owner_type) */
    val index1 = index("idx_applications_owner_id_and_owner_type", (ownerId, ownerType))
  }
  /** Collection-like TableQuery object for table Applications */
  lazy val Applications = new TableQuery(tag => new Applications(tag))

  /** Entity class storing rows of table FollowRequests
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz)
   *  @param accountId Database column account_id SqlType(int8)
   *  @param targetAccountId Database column target_account_id SqlType(int8)
   *  @param showReblogs Database column show_reblogs SqlType(bool), Default(true)
   *  @param uri Database column uri SqlType(text), Default(None)
   *  @param inform Database column inform SqlType(bool), Default(false)
   *  @param languages Database column languages SqlType(_text), Default(None) */
  case class FollowRequestsRow(id: Long, createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime, accountId: Long, targetAccountId: Long, showReblogs: Boolean = true, uri: Option[String] = None, inform: Boolean = false, languages: Option[List[String]] = None)
  /** GetResult implicit for fetching FollowRequestsRow objects using plain SQL queries */
  implicit def GetResultFollowRequestsRow(implicit e0: GR[Long], e1: GR[java.time.ZonedDateTime], e2: GR[Boolean], e3: GR[Option[String]], e4: GR[Option[List[String]]]): GR[FollowRequestsRow] = GR{
    prs => import prs._
    FollowRequestsRow.apply.tupled((<<[Long], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<[Long], <<[Long], <<[Boolean], <<?[String], <<[Boolean], <<?[List[String]]))
  }
  /** Table description of table follow_requests. Objects of this class serve as prototypes for rows in queries. */
  class FollowRequests(_tableTag: Tag) extends profile.api.Table[FollowRequestsRow](_tableTag, "follow_requests") {
    def * = ((id, createdAt, updatedAt, accountId, targetAccountId, showReblogs, uri, inform, languages)).mapTo[FollowRequestsRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(createdAt), Rep.Some(updatedAt), Rep.Some(accountId), Rep.Some(targetAccountId), Rep.Some(showReblogs), uri, Rep.Some(inform), languages)).shaped.<>({r=>import r._; _1.map(_=> FollowRequestsRow.apply.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8.get, _9)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column target_account_id SqlType(int8) */
    val targetAccountId: Rep[Long] = column[Long]("target_account_id")
    /** Database column show_reblogs SqlType(bool), Default(true) */
    val showReblogs: Rep[Boolean] = column[Boolean]("show_reblogs", O.Default(true))
    /** Database column uri SqlType(text), Default(None) */
    val uri: Rep[Option[String]] = column[Option[String]]("uri", O.Default(None))
    /** Database column inform SqlType(bool), Default(false) */
    val inform: Rep[Boolean] = column[Boolean]("inform", O.Default(false))
    /** Database column languages SqlType(_text), Default(None) */
    val languages: Rep[Option[List[String]]] = column[Option[List[String]]]("languages", O.Default(None))

    /** Uniqueness Index over (accountId,targetAccountId) (database name idx_follow_requests_account_id_and_target_account_id) */
    val index1 = index("idx_follow_requests_account_id_and_target_account_id", (accountId, targetAccountId), unique=true)
  }
  /** Collection-like TableQuery object for table FollowRequests */
  lazy val FollowRequests = new TableQuery(tag => new FollowRequests(tag))

  /** Entity class storing rows of table Follows
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz)
   *  @param accountId Database column account_id SqlType(int8)
   *  @param targetAccountId Database column target_account_id SqlType(int8)
   *  @param showReblogs Database column show_reblogs SqlType(bool), Default(true)
   *  @param uri Database column uri SqlType(text), Default(None)
   *  @param inform Database column inform SqlType(bool), Default(false)
   *  @param languages Database column languages SqlType(_text), Default(None) */
  case class FollowsRow(id: Long, createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime, accountId: Long, targetAccountId: Long, showReblogs: Boolean = true, uri: Option[String] = None, inform: Boolean = false, languages: Option[List[String]] = None)
  /** GetResult implicit for fetching FollowsRow objects using plain SQL queries */
  implicit def GetResultFollowsRow(implicit e0: GR[Long], e1: GR[java.time.ZonedDateTime], e2: GR[Boolean], e3: GR[Option[String]], e4: GR[Option[List[String]]]): GR[FollowsRow] = GR{
    prs => import prs._
    FollowsRow.apply.tupled((<<[Long], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<[Long], <<[Long], <<[Boolean], <<?[String], <<[Boolean], <<?[List[String]]))
  }
  /** Table description of table follows. Objects of this class serve as prototypes for rows in queries. */
  class Follows(_tableTag: Tag) extends profile.api.Table[FollowsRow](_tableTag, "follows") {
    def * = ((id, createdAt, updatedAt, accountId, targetAccountId, showReblogs, uri, inform, languages)).mapTo[FollowsRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(createdAt), Rep.Some(updatedAt), Rep.Some(accountId), Rep.Some(targetAccountId), Rep.Some(showReblogs), uri, Rep.Some(inform), languages)).shaped.<>({r=>import r._; _1.map(_=> FollowsRow.apply.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8.get, _9)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column target_account_id SqlType(int8) */
    val targetAccountId: Rep[Long] = column[Long]("target_account_id")
    /** Database column show_reblogs SqlType(bool), Default(true) */
    val showReblogs: Rep[Boolean] = column[Boolean]("show_reblogs", O.Default(true))
    /** Database column uri SqlType(text), Default(None) */
    val uri: Rep[Option[String]] = column[Option[String]]("uri", O.Default(None))
    /** Database column inform SqlType(bool), Default(false) */
    val inform: Rep[Boolean] = column[Boolean]("inform", O.Default(false))
    /** Database column languages SqlType(_text), Default(None) */
    val languages: Rep[Option[List[String]]] = column[Option[List[String]]]("languages", O.Default(None))

    /** Uniqueness Index over (accountId,targetAccountId) (database name idx_follows_account_id_and_target_account_id) */
    val index1 = index("idx_follows_account_id_and_target_account_id", (accountId, targetAccountId), unique=true)
    /** Index over (targetAccountId) (database name idx_follows_target_accountid) */
    val index2 = index("idx_follows_target_accountid", targetAccountId)
  }
  /** Collection-like TableQuery object for table Follows */
  lazy val Follows = new TableQuery(tag => new Follows(tag))

  /** Entity class storing rows of table Media
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param fileName Database column file_name SqlType(text)
   *  @param contentType Database column content_type SqlType(text), Default(None)
   *  @param fileSize Database column file_size SqlType(int8)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param accountId Database column account_id SqlType(int8)
   *  @param blurhash Database column blurhash SqlType(text)
   *  @param processing Database column processing SqlType(bool), Default(false)
   *  @param thumbnailFileName Database column thumbnail_file_name SqlType(text)
   *  @param thumbnailContentType Database column thumbnail_content_type SqlType(text), Default(None)
   *  @param thumbnailFileSize Database column thumbnail_file_size SqlType(int8)
   *  @param url Database column url SqlType(text), Default(None)
   *  @param thumbnailUrl Database column thumbnail_url SqlType(text), Default(None)
   *  @param remoteUrl Database column remote_url SqlType(text), Default(None) */
  case class MediaRow(id: Long, fileName: String, contentType: Option[String] = None, fileSize: Long, createdAt: java.time.ZonedDateTime, accountId: Long, blurhash: String, processing: Boolean = false, thumbnailFileName: String, thumbnailContentType: Option[String] = None, thumbnailFileSize: Long, url: Option[String] = None, thumbnailUrl: Option[String] = None, remoteUrl: Option[String] = None)
  /** GetResult implicit for fetching MediaRow objects using plain SQL queries */
  implicit def GetResultMediaRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[java.time.ZonedDateTime], e4: GR[Boolean]): GR[MediaRow] = GR{
    prs => import prs._
    MediaRow.apply.tupled((<<[Long], <<[String], <<?[String], <<[Long], <<[java.time.ZonedDateTime], <<[Long], <<[String], <<[Boolean], <<[String], <<?[String], <<[Long], <<?[String], <<?[String], <<?[String]))
  }
  /** Table description of table media. Objects of this class serve as prototypes for rows in queries. */
  class Media(_tableTag: Tag) extends profile.api.Table[MediaRow](_tableTag, "media") {
    def * = ((id, fileName, contentType, fileSize, createdAt, accountId, blurhash, processing, thumbnailFileName, thumbnailContentType, thumbnailFileSize, url, thumbnailUrl, remoteUrl)).mapTo[MediaRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(fileName), contentType, Rep.Some(fileSize), Rep.Some(createdAt), Rep.Some(accountId), Rep.Some(blurhash), Rep.Some(processing), Rep.Some(thumbnailFileName), thumbnailContentType, Rep.Some(thumbnailFileSize), url, thumbnailUrl, remoteUrl)).shaped.<>({r=>import r._; _1.map(_=> MediaRow.apply.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10, _11.get, _12, _13, _14)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column file_name SqlType(text) */
    val fileName: Rep[String] = column[String]("file_name")
    /** Database column content_type SqlType(text), Default(None) */
    val contentType: Rep[Option[String]] = column[Option[String]]("content_type", O.Default(None))
    /** Database column file_size SqlType(int8) */
    val fileSize: Rep[Long] = column[Long]("file_size")
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column blurhash SqlType(text) */
    val blurhash: Rep[String] = column[String]("blurhash")
    /** Database column processing SqlType(bool), Default(false) */
    val processing: Rep[Boolean] = column[Boolean]("processing", O.Default(false))
    /** Database column thumbnail_file_name SqlType(text) */
    val thumbnailFileName: Rep[String] = column[String]("thumbnail_file_name")
    /** Database column thumbnail_content_type SqlType(text), Default(None) */
    val thumbnailContentType: Rep[Option[String]] = column[Option[String]]("thumbnail_content_type", O.Default(None))
    /** Database column thumbnail_file_size SqlType(int8) */
    val thumbnailFileSize: Rep[Long] = column[Long]("thumbnail_file_size")
    /** Database column url SqlType(text), Default(None) */
    val url: Rep[Option[String]] = column[Option[String]]("url", O.Default(None))
    /** Database column thumbnail_url SqlType(text), Default(None) */
    val thumbnailUrl: Rep[Option[String]] = column[Option[String]]("thumbnail_url", O.Default(None))
    /** Database column remote_url SqlType(text), Default(None) */
    val remoteUrl: Rep[Option[String]] = column[Option[String]]("remote_url", O.Default(None))

    /** Foreign key referencing Accounts (database name media_account_id_fkey) */
    lazy val accountsFk = foreignKey("media_account_id_fkey", accountId, Accounts)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Media */
  lazy val Media = new TableQuery(tag => new Media(tag))

  /** Entity class storing rows of table Statuses
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param uri Database column uri SqlType(text), Default(None)
   *  @param text Database column text SqlType(text), Default()
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz)
   *  @param inReplyToId Database column in_reply_to_id SqlType(int8), Default(None)
   *  @param reblogOfId Database column reblog_of_id SqlType(int8), Default(None)
   *  @param url Database column url SqlType(text), Default(None)
   *  @param sensitive Database column sensitive SqlType(bool), Default(false)
   *  @param visibility Database column visibility SqlType(int4), Default(0)
   *  @param spoilerText Database column spoiler_text SqlType(text), Default()
   *  @param reply Database column reply SqlType(bool), Default(false)
   *  @param language Database column language SqlType(text), Default(None)
   *  @param conversationId Database column conversation_id SqlType(int8), Default(None)
   *  @param local Database column local SqlType(bool), Default(false)
   *  @param accountId Database column account_id SqlType(int8)
   *  @param inReplyToAccountId Database column in_reply_to_account_id SqlType(int8), Default(None)
   *  @param pollId Database column poll_id SqlType(int8), Default(None)
   *  @param deletedAt Database column deleted_at SqlType(timestamptz), Default(None)
   *  @param editedAt Database column edited_at SqlType(timestamptz), Default(None)
   *  @param mediaAttachmentIds Database column media_attachment_ids SqlType(_int8) */
  case class StatusesRow(id: Long, uri: Option[String] = None, text: String = "", createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime, inReplyToId: Option[Long] = None, reblogOfId: Option[Long] = None, url: Option[String] = None, sensitive: Boolean = false, visibility: Int = 0, spoilerText: String = "", reply: Boolean = false, language: Option[String] = None, conversationId: Option[Long] = None, local: Boolean = false, accountId: Long, inReplyToAccountId: Option[Long] = None, pollId: Option[Long] = None, deletedAt: Option[java.time.ZonedDateTime] = None, editedAt: Option[java.time.ZonedDateTime] = None, mediaAttachmentIds: List[Long])
  /** GetResult implicit for fetching StatusesRow objects using plain SQL queries */
  implicit def GetResultStatusesRow(implicit e0: GR[Long], e1: GR[Option[String]], e2: GR[String], e3: GR[java.time.ZonedDateTime], e4: GR[Option[Long]], e5: GR[Boolean], e6: GR[Int], e7: GR[Option[java.time.ZonedDateTime]], e8: GR[List[Long]]): GR[StatusesRow] = GR{
    prs => import prs._
    StatusesRow.apply.tupled((<<[Long], <<?[String], <<[String], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<?[Long], <<?[Long], <<?[String], <<[Boolean], <<[Int], <<[String], <<[Boolean], <<?[String], <<?[Long], <<[Boolean], <<[Long], <<?[Long], <<?[Long], <<?[java.time.ZonedDateTime], <<?[java.time.ZonedDateTime], <<[List[Long]]))
  }
  /** Table description of table statuses. Objects of this class serve as prototypes for rows in queries. */
  class Statuses(_tableTag: Tag) extends profile.api.Table[StatusesRow](_tableTag, "statuses") {
    def * = ((id, uri, text, createdAt, updatedAt, inReplyToId, reblogOfId, url, sensitive, visibility, spoilerText, reply, language, conversationId, local, accountId, inReplyToAccountId, pollId, deletedAt, editedAt, mediaAttachmentIds)).mapTo[StatusesRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), uri, Rep.Some(text), Rep.Some(createdAt), Rep.Some(updatedAt), inReplyToId, reblogOfId, url, Rep.Some(sensitive), Rep.Some(visibility), Rep.Some(spoilerText), Rep.Some(reply), language, conversationId, Rep.Some(local), Rep.Some(accountId), inReplyToAccountId, pollId, deletedAt, editedAt, Rep.Some(mediaAttachmentIds))).shaped.<>({r=>import r._; _1.map(_=> StatusesRow.apply.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7, _8, _9.get, _10.get, _11.get, _12.get, _13, _14, _15.get, _16.get, _17, _18, _19, _20, _21.get)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column uri SqlType(text), Default(None) */
    val uri: Rep[Option[String]] = column[Option[String]]("uri", O.Default(None))
    /** Database column text SqlType(text), Default() */
    val text: Rep[String] = column[String]("text", O.Default(""))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")
    /** Database column in_reply_to_id SqlType(int8), Default(None) */
    val inReplyToId: Rep[Option[Long]] = column[Option[Long]]("in_reply_to_id", O.Default(None))
    /** Database column reblog_of_id SqlType(int8), Default(None) */
    val reblogOfId: Rep[Option[Long]] = column[Option[Long]]("reblog_of_id", O.Default(None))
    /** Database column url SqlType(text), Default(None) */
    val url: Rep[Option[String]] = column[Option[String]]("url", O.Default(None))
    /** Database column sensitive SqlType(bool), Default(false) */
    val sensitive: Rep[Boolean] = column[Boolean]("sensitive", O.Default(false))
    /** Database column visibility SqlType(int4), Default(0) */
    val visibility: Rep[Int] = column[Int]("visibility", O.Default(0))
    /** Database column spoiler_text SqlType(text), Default() */
    val spoilerText: Rep[String] = column[String]("spoiler_text", O.Default(""))
    /** Database column reply SqlType(bool), Default(false) */
    val reply: Rep[Boolean] = column[Boolean]("reply", O.Default(false))
    /** Database column language SqlType(text), Default(None) */
    val language: Rep[Option[String]] = column[Option[String]]("language", O.Default(None))
    /** Database column conversation_id SqlType(int8), Default(None) */
    val conversationId: Rep[Option[Long]] = column[Option[Long]]("conversation_id", O.Default(None))
    /** Database column local SqlType(bool), Default(false) */
    val local: Rep[Boolean] = column[Boolean]("local", O.Default(false))
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column in_reply_to_account_id SqlType(int8), Default(None) */
    val inReplyToAccountId: Rep[Option[Long]] = column[Option[Long]]("in_reply_to_account_id", O.Default(None))
    /** Database column poll_id SqlType(int8), Default(None) */
    val pollId: Rep[Option[Long]] = column[Option[Long]]("poll_id", O.Default(None))
    /** Database column deleted_at SqlType(timestamptz), Default(None) */
    val deletedAt: Rep[Option[java.time.ZonedDateTime]] = column[Option[java.time.ZonedDateTime]]("deleted_at", O.Default(None))
    /** Database column edited_at SqlType(timestamptz), Default(None) */
    val editedAt: Rep[Option[java.time.ZonedDateTime]] = column[Option[java.time.ZonedDateTime]]("edited_at", O.Default(None))
    /** Database column media_attachment_ids SqlType(_int8) */
    val mediaAttachmentIds: Rep[List[Long]] = column[List[Long]]("media_attachment_ids")

    /** Index over (accountId) (database name idx_statuses_account_id) */
    val index1 = index("idx_statuses_account_id", accountId)
    /** Index over (conversationId) (database name idx_statuses_conversation_id) */
    val index2 = index("idx_statuses_conversation_id", conversationId)
    /** Index over (createdAt) (database name idx_statuses_created_at) */
    val index3 = index("idx_statuses_created_at", createdAt)
    /** Index over (deletedAt) (database name idx_statuses_deleted_at) */
    val index4 = index("idx_statuses_deleted_at", deletedAt)
    /** Index over (editedAt) (database name idx_statuses_edited_at) */
    val index5 = index("idx_statuses_edited_at", editedAt)
    /** Index over (inReplyToAccountId) (database name idx_statuses_in_reply_to_account_id) */
    val index6 = index("idx_statuses_in_reply_to_account_id", inReplyToAccountId)
    /** Index over (inReplyToId) (database name idx_statuses_in_reply_to_id) */
    val index7 = index("idx_statuses_in_reply_to_id", inReplyToId)
    /** Index over (pollId) (database name idx_statuses_poll_id) */
    val index8 = index("idx_statuses_poll_id", pollId)
    /** Index over (reblogOfId) (database name idx_statuses_reblog_of_id) */
    val index9 = index("idx_statuses_reblog_of_id", reblogOfId)
    /** Index over (updatedAt) (database name idx_statuses_updated_at) */
    val index10 = index("idx_statuses_updated_at", updatedAt)
  }
  /** Collection-like TableQuery object for table Statuses */
  lazy val Statuses = new TableQuery(tag => new Statuses(tag))

  /** Entity class storing rows of table Users
   *  @param id Database column id SqlType(int8), AutoInc, PrimaryKey
   *  @param email Database column email SqlType(text), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamptz)
   *  @param updatedAt Database column updated_at SqlType(timestamptz)
   *  @param encryptedPassword Database column encrypted_password SqlType(text), Default()
   *  @param signInCount Database column sign_in_count SqlType(int4), Default(0)
   *  @param lastSignInAt Database column last_sign_in_at SqlType(timestamptz)
   *  @param locale Database column locale SqlType(text), Default(None)
   *  @param lastEmailedAt Database column last_emailed_at SqlType(timestamptz)
   *  @param accountId Database column account_id SqlType(int8)
   *  @param disabled Database column disabled SqlType(bool), Default(false)
   *  @param signUpIp Database column sign_up_ip SqlType(inet), Default(None)
   *  @param timeZone Database column time_zone SqlType(text), Default(None) */
  case class UsersRow(id: Long, email: Option[String] = None, createdAt: java.time.ZonedDateTime, updatedAt: java.time.ZonedDateTime, encryptedPassword: String = "", signInCount: Int = 0, lastSignInAt: java.time.ZonedDateTime, locale: Option[String] = None, lastEmailedAt: java.time.ZonedDateTime, accountId: Long, disabled: Boolean = false, signUpIp: Option[com.github.tminglei.slickpg.InetString] = None, timeZone: Option[String] = None)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Long], e1: GR[Option[String]], e2: GR[java.time.ZonedDateTime], e3: GR[String], e4: GR[Int], e5: GR[Boolean], e6: GR[Option[com.github.tminglei.slickpg.InetString]]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.apply.tupled((<<[Long], <<?[String], <<[java.time.ZonedDateTime], <<[java.time.ZonedDateTime], <<[String], <<[Int], <<[java.time.ZonedDateTime], <<?[String], <<[java.time.ZonedDateTime], <<[Long], <<[Boolean], <<?[com.github.tminglei.slickpg.InetString], <<?[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, "users") {
    def * = ((id, email, createdAt, updatedAt, encryptedPassword, signInCount, lastSignInAt, locale, lastEmailedAt, accountId, disabled, signUpIp, timeZone)).mapTo[UsersRow]
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), email, Rep.Some(createdAt), Rep.Some(updatedAt), Rep.Some(encryptedPassword), Rep.Some(signInCount), Rep.Some(lastSignInAt), locale, Rep.Some(lastEmailedAt), Rep.Some(accountId), Rep.Some(disabled), signUpIp, timeZone)).shaped.<>({r=>import r._; _1.map(_=> UsersRow.apply.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9.get, _10.get, _11.get, _12, _13)))}, (_:Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column email SqlType(text), Default(None) */
    val email: Rep[Option[String]] = column[Option[String]]("email", O.Default(None))
    /** Database column created_at SqlType(timestamptz) */
    val createdAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("created_at")
    /** Database column updated_at SqlType(timestamptz) */
    val updatedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("updated_at")
    /** Database column encrypted_password SqlType(text), Default() */
    val encryptedPassword: Rep[String] = column[String]("encrypted_password", O.Default(""))
    /** Database column sign_in_count SqlType(int4), Default(0) */
    val signInCount: Rep[Int] = column[Int]("sign_in_count", O.Default(0))
    /** Database column last_sign_in_at SqlType(timestamptz) */
    val lastSignInAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("last_sign_in_at")
    /** Database column locale SqlType(text), Default(None) */
    val locale: Rep[Option[String]] = column[Option[String]]("locale", O.Default(None))
    /** Database column last_emailed_at SqlType(timestamptz) */
    val lastEmailedAt: Rep[java.time.ZonedDateTime] = column[java.time.ZonedDateTime]("last_emailed_at")
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column disabled SqlType(bool), Default(false) */
    val disabled: Rep[Boolean] = column[Boolean]("disabled", O.Default(false))
    /** Database column sign_up_ip SqlType(inet), Default(None) */
    val signUpIp: Rep[Option[com.github.tminglei.slickpg.InetString]] = column[Option[com.github.tminglei.slickpg.InetString]]("sign_up_ip", O.Default(None))
    /** Database column time_zone SqlType(text), Default(None) */
    val timeZone: Rep[Option[String]] = column[Option[String]]("time_zone", O.Default(None))

    /** Foreign key referencing Accounts (database name users_account_id_fkey) */
    lazy val accountsFk = foreignKey("users_account_id_fkey", accountId, Accounts)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)

    /** Index over (createdAt) (database name idx_users_created_at) */
    val index1 = index("idx_users_created_at", createdAt)
    /** Index over (lastEmailedAt) (database name idx_users_last_emailed_at) */
    val index2 = index("idx_users_last_emailed_at", lastEmailedAt)
    /** Index over (lastSignInAt) (database name idx_users_last_sign_in_at) */
    val index3 = index("idx_users_last_sign_in_at", lastSignInAt)
    /** Index over (updatedAt) (database name idx_users_updated_at) */
    val index4 = index("idx_users_updated_at", updatedAt)
    /** Uniqueness Index over (accountId) (database name users_account_id_key) */
    val index5 = index("users_account_id_key", accountId, unique=true)
    /** Uniqueness Index over (email) (database name users_email_key) */
    val index6 = index("users_email_key", email, unique=true)
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
