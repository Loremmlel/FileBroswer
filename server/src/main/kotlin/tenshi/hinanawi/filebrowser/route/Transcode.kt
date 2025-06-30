package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.TranscodeRequest
import tenshi.hinanawi.filebrowser.util.TranscodeManager
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

fun Route.transcode(transcoder: TranscodeManager) {
    route("/transcode") {
        post {
            try {
                val request = call.receive<TranscodeRequest>()

                val fullPath = File(AppConfig.basePath, request.filePath).canonicalPath
                if (!fullPath.startsWith(AppConfig.basePath)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        Response<Unit>(403, Message.FilesForbidden, null)
                    )
                    return@post
                }
                val file = File(fullPath)
                if (!file.exists() || file.getFileType() != FileType.Video) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        Response<Unit>(404, Message.TranscodeIsNotVideo, null)
                    )
                    return@post
                }

                val status = transcoder.startTranscode(request)
                call.respond(
                    HttpStatusCode.OK,
                    Response(200, Message.Success, status)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response<Unit>(500, Message.InternalServerError, null)
                )
            }
        }

        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<Unit>(400, Message.TranscodeIdUndefined, null)
                    )
                    return@get
                }
                val status = transcoder.getStatus(id) ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<Unit>(400, Message.TranscodeTaskNotFound, null)
                    )
                    return@get
                }
                call.respond(
                    HttpStatusCode.OK,
                    Response(200, Message.Success, status)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response<Unit>(500, Message.InternalServerError, null)
                )
            }
        }

        delete("/{id}") {
            try {
                val id = call.parameters["id"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<Unit>(400, Message.TranscodeIdUndefined, null)
                    )
                    return@delete
                }
                val success = transcoder.stopTranscode(id)
                if (success) {
                    call.respond(
                        HttpStatusCode.OK,
                        Response(200, Message.Success, null)
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<Unit>(400, Message.TranscodeTaskNotFound, null)
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response<Unit>(500, Message.InternalServerError, null)
                )
            }
        }

        get("/status") {
            call.respond(
                HttpStatusCode.OK,
                Response(200, Message.Success, mapOf(
                    "activeTasks" to transcoder.activeTasks.value
                ))
            )
        }
    }
}