package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

fun Route.video() {
    route("/video") {
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

                val contentType = when (file.extension.lowercase()) {
                    "m3u8" -> ContentType.parse("application/vnd.apple.mpegurl")
                    "ts" -> ContentType.parse("video/mp2t")
                    else -> ContentType.Application.OctetStream
                }
                response.header("Content-Type", contentType.toString())
                respondFile(file)
            }
        }
    }
}