package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.application.*
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
import tenshi.hinanawi.filebrowser.util.requestError

internal fun Application.files() = routing {
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
          if (it.isHidden) {
            return@forEach
          }
          res.add(
            FileInfo(
              it.name,
              it.length(),
              it.isDirectory,
              it.getFileType(),
              it.lastModified(),
              it.path.substring(AppConfig.BASE_DIR.length)
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
        log.requestError(call, e)
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
          log.warn("文件${file.path}删除失败")
          return@delete
        }
        call.respond(
          HttpStatusCode.NoContent,
          Response(204, Message.Success, null)
        )
      } catch (e: Exception) {
        call.respond(
          HttpStatusCode.InternalServerError,
          Response(500, Message.InternalServerError, null)
        )
        log.requestError(call, e)
      }
    }
  }
}
