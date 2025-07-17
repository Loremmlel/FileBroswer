package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.exception.ApiException
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus
import tenshi.hinanawi.filebrowser.util.ErrorHandler

interface TranscodeRepository {
  suspend fun startTranscode(path: String): TranscodeStatus
  fun observeTranscode(id: String): Flow<TranscodeStatus?>
  suspend fun stopTranscode(id: String): Boolean
}

class OnlineTranscodeRepository : TranscodeRepository, BaseOnlineRepository() {
  private val basePath = "/transcode"
  override suspend fun startTranscode(path: String): TranscodeStatus = try {
    val response = client.post("$basePath?path=$path").body<Response<TranscodeStatus>>()
    response.data ?: throw ApiException(response.code, response.message)
  } catch (e: Exception) {
    throw Exception("转码启动失败: ${e.message}")
  }

  override fun observeTranscode(id: String): Flow<TranscodeStatus?> = flow {
    while (true) {
      val response = client.get("$basePath/$id").body<Response<TranscodeStatus>>()
      emit(response.data)
      delay(1500)
    }
  }

  override suspend fun stopTranscode(id: String): Boolean = try {
    client.delete("$basePath/$id").status == HttpStatusCode.OK
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }
}