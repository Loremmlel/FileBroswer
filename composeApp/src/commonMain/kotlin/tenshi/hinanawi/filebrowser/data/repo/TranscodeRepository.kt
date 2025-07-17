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

interface TranscodeRepository {
  fun startTranscode(path: String): Flow<TranscodeStatus>
  fun observeTranscode(id: String): Flow<TranscodeStatus?>
  suspend fun stopTranscode(id: String): Boolean
}

class OnlineTranscodeRepository : TranscodeRepository, BaseOnlineRepository() {
  private val basePath = "/transcode"
  override fun startTranscode(path: String): Flow<TranscodeStatus> = flow {
    val response = client.post("$basePath?path=$path").body<Response<TranscodeStatus>>()
    emit(response.data ?: throw ApiException(code = response.code, message = response.message))
  }

  override fun observeTranscode(id: String): Flow<TranscodeStatus?> = flow {
    while (true) {
      val response = client.get("$basePath/$id").body<Response<TranscodeStatus>>()
      emit(response.data)
      delay(500)
    }
  }

  override suspend fun stopTranscode(id: String): Boolean = try {
    client.delete("$basePath/$id").status == HttpStatusCode.OK
  } catch (_: Exception) {
    false
  }
}