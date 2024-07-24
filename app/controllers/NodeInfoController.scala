package controllers

import play.api.libs.json.*
import play.api.mvc.*
import javax.inject.Inject

// wip

class NodeInfoController @Inject() (cc: ControllerComponents)
    extends AbstractController(cc) {
  val wellKnown: Action[AnyContent] = Action {
    Function.const(
      Ok(
        Json.obj(
          "links" -> Json.arr(
            Json.obj(
              "rel" -> "http://nodeinfo.diaspora.software/ns/schema/2.0",
              "href" -> "/nodeinfo/2.0"
            )
          )
        )
      )
    )
  }

  val nodeInfo: Action[AnyContent] = Action {
    Function.const(
      Ok(
        Json.obj(
          "openRegisterations" -> true,
          "protocols" -> Json.arr(),
          "software" -> Json.obj("name" -> "Akatsuki", "version" -> "0.0.1"),
          "usage" -> Json.obj("users" -> Json.obj("total" -> 0)),
          "version" -> "2.0",
          "metadata" -> Json.obj(
            "nodeName" -> "Akatsuki",
            "nodeDescription" -> "Akatsuki is a decentralized social network.",
            "nodeAdmins" -> Json.arr(),
            "maintainer" -> Json.obj(),
            "langs" -> Json.arr()
          )
        )
      )
    )
  }
}
