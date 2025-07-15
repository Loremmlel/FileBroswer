package tenshi.hinanawi.filebrowser.data.online

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import tenshi.hinanawi.filebrowser.component.yuzu.RemoteImageState
import tenshi.hinanawi.filebrowser.data.repo.ImageRepository

class OnlineImageRepository : BaseOnlineRepository(), ImageRepository {
  // fix: Kotlin Flow 的并发发射限制：普通 flow 构建器不允许从多个协程并发发射数据
  // 解决方案：使用 callbackFlow
  override fun getImageStream(path: String): Flow<RemoteImageState> = callbackFlow {
    trySend(RemoteImageState.Loading)
    val response = client.get("/image?path=$path") {
      onDownload { bytesSentTotal, contentLength ->
        if (contentLength != null) {
          val progress = bytesSentTotal.toFloat() / contentLength
          trySend(RemoteImageState.Progress(progress))
        }
      }
    }
    if (response.headers[HttpHeaders.ContentType]?.startsWith("image/") != true) {
      trySend(RemoteImageState.Error("服务器错误: ${response.status}"))
      close()
      return@callbackFlow
    }

    // 流式读取图片数据
    val channel = response.bodyAsChannel()
    val chunks = mutableListOf<ByteArray>()
    var totalSize = 0

    val buffer = ByteArray(8192) // 8KB 缓冲区
    while (!channel.isClosedForRead) {
      val bytesRead = channel.readAvailable(buffer)
      if (bytesRead > 0) {
        val chunk = buffer.copyOf(bytesRead)
        chunks.add(chunk)
        totalSize += bytesRead
      } else if (bytesRead == -1) {
        break
      }
    }

    // 合并所有数据块
    val imageData = ByteArray(totalSize)
    var offset = 0
    chunks.forEach { chunk ->
      chunk.copyInto(imageData, offset)
      offset += chunk.size
    }

    val imageBitmap = imageData.decodeToImageBitmap()
    val painter = BitmapPainter(imageBitmap)
    trySend(RemoteImageState.Success(painter))
    close()
  }
}