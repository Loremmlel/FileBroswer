package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.util.ErrorHandler

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