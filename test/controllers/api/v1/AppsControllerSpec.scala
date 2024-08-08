package controllers.api.v1

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.*
import play.api.test.Helpers.*
import repositories.{AuthRepository, Tables}
import slick.dbio.DBIO

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class AppsControllerSpec
    extends PlaySpec
    with MockitoSugar
    with Results
    with GuiceOneServerPerSuite {
  "AppsController" should {

    "create an app successfully" in {

      given Materializer = Materializer(ActorSystem("TestSystem"))

      val dbConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]

      val instantExpected = "2024-01-01T00:00:00Z"

      val mockControllerComponents = stubControllerComponents()

      val testApp = Tables.ApplicationsRow(
        id = 1L,
        name = "test-client",
        secret = "secret",
        redirectUri = "http://example.com/redirect",
        scopes = "read",
        code = None,
        createdAt = ZonedDateTime.parse(instantExpected),
        updatedAt = ZonedDateTime.parse(instantExpected),
        website = Some("http://example.com")
      )

      val mockAuthRepo = mock[AuthRepository]

      when(
        mockAuthRepo.createApp(
          "test-client",
          "http://example.com/redirect",
          Some("read"),
          Some("http://example.com")
        )
      ).thenReturn(DBIO.successful(testApp))

      val appsController =
        new AppsController(
          mockControllerComponents,
          dbConfigProvider,
          mockAuthRepo
        )

      val request = FakeRequest(POST, "/apps").withFormUrlEncodedBody(
        "client_name" -> "test-client",
        "redirect_uris" -> "http://example.com/redirect",
        "scopes" -> "read",
        "website" -> "http://example.com"
      )

      val result = TestUtils.callWithFilter(appsController.apps, request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "id" -> "1",
        "name" -> "test-client",
        "website" -> "http://example.com",
        "redirect_uri" -> "http://example.com/redirect",
        "client_id" -> "1",
        "client_secret" -> "secret"
      )
    }

    "handle repository error gracefully" in {
      given Materializer = Materializer(ActorSystem("TestSystem"))

      val dbConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]

      val mockControllerComponents = stubControllerComponents()

      val mockAuthRepo = mock[AuthRepository]

      when(
        mockAuthRepo.createApp(
          "test-client",
          "http://example.com/redirect",
          Some("read"),
          Some("http://example.com")
        )
      ).thenReturn(DBIO.failed(Exception("Database error")))

      val appsController =
        new AppsController(
          mockControllerComponents,
          dbConfigProvider,
          mockAuthRepo
        )

      val request = FakeRequest(POST, "/apps").withFormUrlEncodedBody(
        "client_name" -> "test-client",
        "redirect_uris" -> "http://example.com/redirect",
        "scopes" -> "read",
        "website" -> "http://example.com"
      )

      val result = TestUtils.callWithFilter(appsController.apps, request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "error" -> "Internal server error occurred"
      )
    }
  }
}
