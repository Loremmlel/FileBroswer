package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.model.response.FileType
import tenshi.hinanawi.filebrowser.model.response.parseFileType
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

internal fun Route.random() {
  route("/random") {
    install(PathValidator)
    // fix: 操，忘了删这玩意儿，导致测试失败了
    // 单元测试真有用好吧
    get {
      call.safeExecute {
        contentTypeJson()
        val type = call.queryParameters["type"]?.parseFileType() ?: FileType.Video
        val dir = call.attributes[ValidatedFileKey]
        val files = scanDirectory(dir, type)
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, files)
        )
      }
    }
  }
}

private fun scanDirectory(dir: File, type: FileType): List<FileInfo> {
  val res = mutableListOf<FileInfo>()
  if (!dir.isDirectory) {
    return res
  }
  for (file in dir.listFiles() ?: emptyArray()) {
    if (file.isDirectory) {
      res.addAll(scanDirectory(file, type))
      continue
    }
    if (file.getFileType() != type) {
      continue
    }
    res.add(
      FileInfo(
        name = file.name,
        size = file.length(),
        isDirectory = file.isDirectory,
        type = file.getFileType(),
        lastModified = file.lastModified(),
        path = file.path.substring(AppConfig.basePath.length).replace("\\", "/")
      )
    )
  }
  return res
}