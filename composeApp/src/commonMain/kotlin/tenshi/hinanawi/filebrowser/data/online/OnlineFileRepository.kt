package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.SERVER_URL
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.ResponseWithoutData
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineFileRepository : BaseOnlineRepository(), FilesRepository {
  override fun getFiles(path: String) = flow {
    try {
      val response = client.get("$SERVER_URL/files?path=$path").body<Response<List<FileInfo>>>()
      emit(response.data ?: emptyList())
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
      emptyList<List<FileInfo>>()
    }
  }

  override suspend fun deleteFile(path: String) = try {
    client.delete("$SERVER_URL/files?path=$path")
    Unit
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
  }
}