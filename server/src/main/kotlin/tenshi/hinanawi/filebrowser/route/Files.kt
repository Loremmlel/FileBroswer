package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getContentType
import tenshi.hinanawi.filebrowser.util.getFileType

internal fun Route.files() {
  route("/files") {
    install(PathValidator)
    install(PartialContent)
    get {
      call.safeExecute {
        contentTypeJson()
        val dir = attributes[ValidatedFileKey]
        if (!dir.isDirectory) {
          respond(
            HttpStatusCode.BadRequest,
            Response<FileInfo>(400, Message.FilesIsNotDirectory, null)
          )
          return@safeExecute
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
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, res)
        )
      }
    }
    delete {
      call.safeExecute {
        contentTypeJson()
        val file = attributes[ValidatedFileKey]
        if (!file.delete()) {
          if (file.isDirectory) {
            respond(
              HttpStatusCode.BadRequest,
              Response(400, Message.FilesDirectoryMustEmptyWhileDelete, null)
            )
            return@safeExecute
          }
          respond(
            HttpStatusCode.InternalServerError,
            Response(500, Message.Failed, null)
          )
          return@safeExecute
        }
        // 当响应体为204 No Content时，响应体为空，会导致序列化问题
        respond(
          HttpStatusCode.OK,
          Response(204, Message.Success, null)
        )
      }
    }
    get("/download") {
      call.safeExecute {
        val file = attributes[ValidatedFileKey]
        if (file.isDirectory) {
          respond(
            HttpStatusCode.BadRequest,
            Response<Unit>(400, Message.FilesCannotDownloadDirectory, null)
          )
          return@safeExecute
        }
        response.header(
          HttpHeaders.ContentDisposition,
          ContentDisposition.Attachment
            .withParameter(ContentDisposition.Parameters.FileName, file.name)
            .toString()
        )
        response.header(
          HttpHeaders.ContentType,
          file.getContentType()
        )
        respondFile(file)
      }
    }
  }
}
