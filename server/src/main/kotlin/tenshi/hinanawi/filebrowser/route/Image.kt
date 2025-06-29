package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getContentType
import tenshi.hinanawi.filebrowser.util.getFileType
import tenshi.hinanawi.filebrowser.util.requestError

internal fun Application.image() = routing {
  route("/image") {
    install(PathValidator)
    get {
      try {
        val file = call.attributes[ValidatedFileKey]

        if (file.getFileType() != FileType.Image) {
          call.contentTypeJson()
          call.respond(
            HttpStatusCode.BadRequest,
            Response(400, Message.ImageIsNotImage, null)
          )
          return@get
        }

        val contentType = file.getContentType()
        call.response.header("Content-Type", contentType)
        call.response.header("Content-Length", file.length())

        file.inputStream().use { inputStream ->
          call.respond(inputStream)
        }
      } catch (e: Exception) {
        call.contentTypeJson()
        call.respond(
          HttpStatusCode.InternalServerError,
          Response(500, Message.InternalServerError, null)
        )
        log.requestError(call, e)
      }
    }
  }
}
