package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

fun Route.video() {
    route("/video") {
        get("/{taskId}/{...}") {
            try {
                val taskId = call.parameters["taskId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(400, Message.VideoTaskIdUndefined, null)
                    )
                    return@get
                }
                val pathSegments = call.parameters.getAll("...")?.joinToString("/") ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(400, Message.VideoPathSegmentUndefined, null)
                    )
                    return@get
                }

                val filePath = File("${AppConfig.cachePath}/$taskId", pathSegments).canonicalPath
                if (!filePath.startsWith(AppConfig.cachePath)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        Response(403, Message.FilesForbidden, null)
                    )
                    return@get
                }

                val file = File(filePath)
                if (!file.exists() || file.getFileType() != FileType.Video) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        Response(404, Message.VideoIsNotVideo, null)
                    )
                    return@get
                }

                val contentType = when (file.extension.lowercase()) {
                    "m3u8" -> ContentType.parse("application/vnd.apple.mpegurl")
                    "ts" -> ContentType.parse("video/mp2t")
                    else -> ContentType.Application.OctetStream
                }
                call.response.header("Content-Type", contentType.toString())
                call.respondFile(file)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response(500, Message.InternalServerError, null)
                )
            }
        }
    }
}