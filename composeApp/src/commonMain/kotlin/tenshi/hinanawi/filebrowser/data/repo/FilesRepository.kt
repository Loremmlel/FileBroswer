package tenshi.hinanawi.filebrowser.data.repo

import tenshi.hinanawi.filebrowser.model.FileInfo

interface FilesRepository {
    suspend fun getFiles(path: String): List<FileInfo>

    suspend fun deleteFile(path: String)
}