package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getContentType
import tenshi.hinanawi.filebrowser.util.getFileType

internal fun Route.image() {
  route("/image") {
    install(PathValidator)
    get {
      call.safeExecute {
        val file = attributes[ValidatedFileKey]

        if (file.getFileType() != FileType.Image) {
          contentTypeJson()
          respond(
            HttpStatusCode.BadRequest,
            Response(400, Message.ImageIsNotImage, null)
          )
          return@safeExecute
        }

        val contentType = file.getContentType()
        response.header(HttpHeaders.ContentType, contentType)
        response.header(HttpHeaders.ContentLength, file.length())

        file.inputStream().use { inputStream ->
          respond(inputStream)
        }
      }
    }
  }
}
