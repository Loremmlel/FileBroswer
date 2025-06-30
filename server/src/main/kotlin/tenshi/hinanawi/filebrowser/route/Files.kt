package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getFileType

internal fun Route.files() = {
  route("/files") {
    install(PathValidator)
    get {
      try {
        call.contentTypeJson()
        val dir = call.attributes[ValidatedFileKey]
        if (!dir.isDirectory) {
          call.respond(
            HttpStatusCode.BadRequest,
            Response<FileInfo>(400, Message.FilesIsNotDirectory, null)
          )
          return@get
        }
        val res = mutableListOf<FileInfo>()
        dir.listFiles().forEach {
          if (it.isHidden || it.name == AppConfig.CACHE_DIR_NAME) {
            return@forEach
          }
          res.add(
            FileInfo(
              it.name,
              it.length(),
              it.isDirectory,
              it.getFileType(),
              it.lastModified(),
              it.path.substring(AppConfig.basePath.length).replace("\\", "/")
            )
          )
        }
        res.sortWith(Comparator { a, b ->
          if (a.isDirectory && !b.isDirectory) {
            -1
          } else if (!a.isDirectory && b.isDirectory) {
            1
          } else {
            a.name.compareTo(b.name)
          }
        })
        call.respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, res)
        )
      } catch (e: Exception) {
        call.respond(
          HttpStatusCode.InternalServerError,
          Response(500, Message.InternalServerError, null)
        )
      }
    }
    delete {
      try {
        call.contentTypeJson()
        val file = call.attributes[ValidatedFileKey]
        if (!file.delete()) {
          if (file.isDirectory) {
            call.respond(
              HttpStatusCode.BadRequest,
              Response(400, Message.FilesDirectoryMustEmptyWhileDelete, null)
            )
            return@delete
          }
          call.respond(
            HttpStatusCode.InternalServerError,
            Response(500, Message.Failed, null)
          )
          return@delete
        }
        // 当响应体为204 No Content时，响应体为空，会导致序列化问题
        call.respond(
          HttpStatusCode.OK,
          Response(204, Message.Success, null)
        )
      } catch (e: Exception) {
        call.respond(
          HttpStatusCode.InternalServerError,
          Response(500, Message.InternalServerError, null)
        )
      }
    }
  }
}
