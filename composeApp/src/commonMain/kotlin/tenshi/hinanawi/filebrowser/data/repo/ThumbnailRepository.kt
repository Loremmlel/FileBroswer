package tenshi.hinanawi.filebrowser.data.repo

import androidx.compose.ui.graphics.ImageBitmap

interface ThumbnailRepository {
  suspend fun getThumbnail(path: String): ImageBitmap?
}