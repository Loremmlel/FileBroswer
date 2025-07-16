package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.RandomRepository
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.model.Response

class OnlineRandomRepository : RandomRepository, BaseOnlineRepository() {
  override fun getAllVideo(path: String): Flow<List<FileInfo>> = flow {
    val response = client.get("/random?path=$path").body<Response<List<FileInfo>>>()
    emit(response.data ?: emptyList())
  }
}