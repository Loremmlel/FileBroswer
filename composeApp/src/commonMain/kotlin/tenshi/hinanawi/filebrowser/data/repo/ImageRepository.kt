package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.viewmodel.ImageLoadState

interface ImageRepository {
  fun getImageStream(path: String): Flow<ImageLoadState>
}