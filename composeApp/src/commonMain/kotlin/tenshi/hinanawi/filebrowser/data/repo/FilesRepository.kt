package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import tenshi.hinanawi.filebrowser.SERVER_URL
import tenshi.hinanawi.filebrowser.getPlatform
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.platform.createFileDownloader
import tenshi.hinanawi.filebrowser.util.ErrorHandler

interface FilesRepository {
  suspend fun getFiles(path: String): List<FileInfo>

  suspend fun deleteFile(path: String)

  suspend fun downloadFile(path: String, filename: String)
}

class OnlineFileRepository : BaseOnlineRepository(), FilesRepository {
  private val basePath = "/files"

  private val fileDownloader = createFileDownloader()

  override suspend fun getFiles(path: String): List<FileInfo> {
    val response = client.get("$basePath?path=$path").body<Response<List<FileInfo>>>()
    return response.data ?: emptyList()
  }

  override suspend fun deleteFile(path: String) = try {
    client.delete("$basePath?path=$path")
    Unit
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
  }

  override suspend fun downloadFile(path: String, filename: String) {
    try {
      val downloadUrl = "$SERVER_URL$basePath/download?path=$path"
      if (getPlatform().name == "Web with Kotlin/Wasm") {
        fileDownloader.downloadFile(downloadUrl, filename)
        return
      }
      val response = client.get("$basePath/download?path=$path")
      val contentLength = response.headers["Content-Length"]?.toLongOrNull()

      // 使用流式下载
      fileDownloader.downloadFile(downloadUrl, filename, response.bodyAsChannel(), contentLength)
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
    }
  }
}