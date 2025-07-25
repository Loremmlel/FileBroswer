package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.*
import io.ktor.client.request.*
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileInfo

interface RandomRepository {
  suspend fun getAllVideo(path: String): List<FileInfo>
}

class OnlineRandomRepository : RandomRepository, BaseOnlineRepository() {
  private val basePath = "/random"
  override suspend fun getAllVideo(path: String): List<FileInfo> {
    val response = client.get("$basePath?path=$path").body<Response<List<FileInfo>>>()
    return response.data ?: emptyList()
  }
}