package tenshi.hinanawi.filebrowser.data.online

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import tenshi.hinanawi.filebrowser.data.repo.ThumbnailRepository
import tenshi.hinanawi.filebrowser.platform.toImageBitmap
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineThumbnailRepository : ThumbnailRepository, BaseOnlineRepository() {
  private val _cache = LruCache<String, ImageBitmap>(100)
  override suspend fun getThumbnail(path: String): ImageBitmap? {
    val cache = _cache[path]
    if (cache != null) {
      return cache
    }
    return try {
      val byteArray = client.get("/thumbnail?path=$path").bodyAsBytes()
      if (byteArray.isEmpty()) {
        null
      }
      val imageBitmap = byteArray.toImageBitmap()
      _cache.put(path, imageBitmap)
      imageBitmap
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
      null
    }
  }
}