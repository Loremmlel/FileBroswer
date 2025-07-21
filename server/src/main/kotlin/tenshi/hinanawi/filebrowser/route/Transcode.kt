package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileType
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.service.TranscodeService
import tenshi.hinanawi.filebrowser.util.getFileType

fun Route.transcode(
  transcodeService: TranscodeService
) {
  route("/transcode") {
    install(PathValidator)
    post {
      call.safeExecute {
        val video = attributes[ValidatedFileKey]
        if (video.getFileType() != FileType.Video) {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeIsNotVideo, null)
          )
          return@safeExecute
        }

        val status = transcodeService.startTranscode(video)
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, status)
        )
      }
    }
  }

  // 如果使用"/transocde/{id}"，依然会应用PathValidator插件
  route("/transcoding/{id}") {
    get {
      call.safeExecute {
        val id = call.pathParameters["id"]
        if (id == null) {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeIdUndefined, null)
          )
          return@safeExecute
        }

        val status = transcodeService.getStatus(id)
        if (status == null) {
          respond(
            HttpStatusCode.NotFound,
            Response<Unit>(404, Message.TranscodeTaskNotFound, null)
          )
          return@safeExecute
        }
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, status)
        )
      }
    }

    delete {
      call.safeExecute {
        val id = call.pathParameters["id"]
        if (id == null) {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.TranscodeIdUndefined, null)
          )
          return@safeExecute
        }

        transcodeService.stopTranscode(id)
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, null)
        )
      }
    }
  }
}