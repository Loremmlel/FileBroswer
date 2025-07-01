package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.model.FileInfo

interface RandomRepository {
  fun getAllVideo(path: String): Flow<List<FileInfo>>
}