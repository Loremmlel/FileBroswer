package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineFileRepository : BaseOnlineRepository(), FilesRepository {
  override fun getFiles(path: String) = flow {
    val response = client.get("/files?path=$path").body<Response<List<FileInfo>>>()
    emit(response.data ?: emptyList())
  }.catch {
    ErrorHandler.handleException(it)
  }

  override suspend fun deleteFile(path: String) = try {
    client.delete("/files?path=$path")
    Unit
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
  }
}