package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.getContentType
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

fun Route.video() {
  route("/video") {
    install(PartialContent)
    get("/{taskId}/{...}") {
      call.safeExecute {
        val taskId = parameters["taskId"] ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response(400, Message.VideoTaskIdUndefined, null)
          )
          return@safeExecute
        }
        val pathSegments = parameters.getAll("...")?.joinToString("/") ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response(400, Message.VideoPathSegmentUndefined, null)
          )
          return@safeExecute
        }

        val filePath = File("${AppConfig.cachePath}/$taskId", pathSegments).canonicalPath
        if (!filePath.startsWith(AppConfig.cachePath)) {
          respond(
            HttpStatusCode.Forbidden,
            Response(403, Message.FilesForbidden, null)
          )
          return@safeExecute
        }

        val file = File(filePath)
        if (!file.exists() || file.getFileType() != FileType.Video) {
          respond(
            HttpStatusCode.NotFound,
            Response(404, Message.VideoIsNotVideo, null)
          )
          return@safeExecute
        }
        when (file.extension.lowercase()) {
          "m3u8" -> {
            response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            response.header(HttpHeaders.Pragma, "no-cache")
            response.header(HttpHeaders.Expires, "0")
          }

          "ts" -> {
            response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            response.header(HttpHeaders.ETag, "\"${file.lastModified()}-${file.length()}\"")
          }

          else -> {
            response.header(HttpHeaders.CacheControl, "public, max-age=3600")
            response.header(HttpHeaders.ETag, "\"${file.lastModified()}-${file.length()}\"")
          }
        }
        val contentType = file.getContentType()
        response.header("Content-Type", contentType)
        respondFile(file)
      }
    }
  }
}