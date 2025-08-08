package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileType
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.getCacheControl
import tenshi.hinanawi.filebrowser.util.getContentType
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File
import java.nio.file.Paths

fun Route.video() {
  route("/video") {
    install(PartialContent)
    /**
     * 获取HLS视频流接口
     *
     * 例如，/one-uuid/playlist.m3u8、/one-uuid/segment0001.ts等等。
     */
    get("/{taskId}/{param...}") {
      call.safeExecute {
        val taskId = call.pathParameters["taskId"] ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.VideoTaskIdUndefined, null)
          )
          return@safeExecute
        }
        println(taskId)
        // 不能使用{...}然后getAll("...")或者getAll("")，会获取不到数据
        val segments = call.pathParameters.getAll("param")?.joinToString("/") ?: run {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.VideoPathSegmentUndefined, null)
          )
          return@safeExecute
        }
        val cacheDir = File(AppConfig.cachePath, taskId)
        val file = File(cacheDir, segments)

        // windows的反斜杠!!!
        val path = Paths.get(file.canonicalPath)
        if (!path.normalize().startsWith(AppConfig.basePath)) {
          respond(
            HttpStatusCode.Forbidden,
            Response<Unit>(403, Message.FilesForbidden, null)
          )
          return@safeExecute
        }

        if (!file.exists() || file.isDirectory) {
          respond(
            HttpStatusCode.NotFound,
            Response<Unit>(404, Message.FilesNotFound, null)
          )
          return@safeExecute
        }

        response.header(HttpHeaders.ContentType, file.getContentType())
        response.header(HttpHeaders.CacheControl, file.getCacheControl())
        respondFile(file)
      }
    }
  }

  /**
   * 直接播放视频的接口
   */
  route("/direct-video") {
    install(PathValidator)
    install(PartialContent)
    get {
      call.safeExecute {
        val file = attributes[ValidatedFileKey]
        if (file.getFileType() != FileType.Video) {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.VideoIsNotVideo, null)
          )
          return@safeExecute
        }
        response.header(HttpHeaders.ContentType, file.getContentType())
        response.header(HttpHeaders.CacheControl, file.getCacheControl())
        respondFile(file)
      }
    }
  }
}