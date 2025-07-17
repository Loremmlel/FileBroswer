package tenshi.hinanawi.filebrowser.data.repo

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import tenshi.hinanawi.filebrowser.platform.toImageBitmap

interface ThumbnailRepository {
  suspend fun getThumbnail(path: String): ImageBitmap?
}

class OnlineThumbnailRepository : ThumbnailRepository, BaseOnlineRepository() {
  private val basePath = "/thumbnail"
  private val _cache = LruCache<String, ImageBitmap>(100)
  override suspend fun getThumbnail(path: String): ImageBitmap? {
    val cache = _cache[path]
    if (cache != null) {
      return cache
    }
    return try {
      val byteArray = client.get("${basePath}?path=$path").bodyAsBytes()
      if (byteArray.isEmpty()) {
        null
      }
      val imageBitmap = byteArray.toImageBitmap()
      _cache.put(path, imageBitmap)
      imageBitmap
    } catch (_: Exception) {
      null
    }
  }
}