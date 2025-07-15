package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.component.yuzu.RemoteImageState

interface ImageRepository {
  fun getImageStream(path: String): Flow<RemoteImageState>
}