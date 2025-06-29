package tenshi.hinanawi.filebrowser.data.online

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.ImageRepository
import tenshi.hinanawi.filebrowser.viewmodel.ImageLoadState

class OnlineImageRepository: BaseOnlineRepository(), ImageRepository {
  override fun getImageStream(path: String): Flow<ImageLoadState> = flow {
    val response = client.get("/image?path=$path") {
      onDownload { bytesSentTotal, contentLength ->
        if (contentLength != null) {
          val progress = bytesSentTotal.toFloat() / contentLength
          emit(ImageLoadState.Progress(progress))
        }
      }
    }
    if (response.headers[HttpHeaders.ContentType]?.startsWith("image/") != true) {
      emit(ImageLoadState.Error("服务器错误: ${response.status}"))
      return@flow
    }
    val imageBitmap = response.bodyAsChannel().toByteArray().decodeToImageBitmap()
    val painter = BitmapPainter(imageBitmap)
    emit(ImageLoadState.Success(painter))
  }.catch { throwable ->
    emit(ImageLoadState.Error("flow错误: ${throwable.message}"))
  }
}