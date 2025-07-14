package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.schema.ThumbnailService

fun Route.thumbnail() {
  val thumbnailService = ThumbnailService()
  route("/thumbnail") {
    install(PathValidator)
    get("/thumbnail") {
      call.safeExecute {
        val file = call.attributes[ValidatedFileKey]
        val thumbnailData = thumbnailService.createThumbnail(file)
        if (thumbnailData == null) {
          respond(
            HttpStatusCode.InternalServerError,
            Response<Unit>(500, Message.InternalServerError, null)
          )
          return@safeExecute
        }
        response.header(HttpHeaders.ContentType, "image/jpeg")
        response.header(HttpHeaders.CacheControl, "public, max-age=86400")
        respondBytes(thumbnailData)
      }
    }
  }
}