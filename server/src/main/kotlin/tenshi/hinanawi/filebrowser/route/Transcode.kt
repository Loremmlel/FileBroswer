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
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.TranscodeManager
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

fun Route.transcode(transcoder: TranscodeManager) {
  route("/transcode") {
    post {
      call.safeExecute {
        val request = receive<TranscodeRequest>()

        val fullPath = File(AppConfig.basePath, request.filePath).canonicalPath
        if (!fullPath.startsWith(AppConfig.basePath)) {
          respond(
            HttpStatusCode.Forbidden,
            Response<Unit>(403, Message.FilesForbidden, null)
          )
          return@safeExecute
        }
        val file = File(fullPath)
        if (!file.exists() || file.getFileType() != FileType.Video) {
          respond(
            HttpStatusCode.NotFound,
            Response<Unit>(404, Message.TranscodeIsNotVideo, null)
          )
          return@safeExecute
        }

        val status = transcoder.startTranscode(request)
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, status)
        )
      }
    }

    get("/{id}") {
      call.safeExecute {
        val id = parameters["id"] ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeIdUndefined, null)
          )
          return@safeExecute
        }
        val status = transcoder.getStatus(id) ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeTaskNotFound, null)
          )
          return@safeExecute
        }
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, status)
        )
      }
    }

    delete("/{id}") {
      call.safeExecute {
        val id = parameters["id"] ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeIdUndefined, null)
          )
          return@safeExecute
        }
        val success = transcoder.stopTranscode(id)
        if (success) {
          respond(
            HttpStatusCode.OK,
            Response(200, Message.Success, null)
          )
        } else {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeTaskNotFound, null)
          )
        }
      }
    }

    get("/status") {
      call.safeExecute {
        respond(
          HttpStatusCode.OK,
          Response(
            200, Message.Success, mapOf(
              "activeTasks" to transcoder.activeTasks.value
            )
          )
        )
      }
    }
  }
}