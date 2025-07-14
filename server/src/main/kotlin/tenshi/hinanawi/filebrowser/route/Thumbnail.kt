package tenshi.hinanawi.filebrowser.route

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import tenshi.hinanawi.filebrowser.schema.ThumbnailService

fun Route.thumbnail() {
  val thumbnailService = ThumbnailService()
  route("/thumbnail") {

  }
}