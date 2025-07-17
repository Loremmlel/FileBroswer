package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.FileInfo

interface RandomRepository {
  fun getAllVideo(path: String): Flow<List<FileInfo>>
}

class OnlineRandomRepository : RandomRepository, BaseOnlineRepository() {
  override fun getAllVideo(path: String): Flow<List<FileInfo>> = flow {
    val response = client.get("/random?path=$path").body<Response<List<FileInfo>>>()
    emit(response.data ?: emptyList())
  }
}