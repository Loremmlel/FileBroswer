package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.model.FileInfo

interface FilesRepository {
  fun getFiles(path: String): Flow<List<FileInfo>>

  suspend fun deleteFile(path: String)

  suspend fun downloadFile(path: String, filename: String)
}