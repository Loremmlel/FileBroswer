package tenshi.hinanawi.filebrowser.model.response

import kotlinx.serialization.Serializable
import tenshi.hinanawi.filebrowser.model.request.AddFileToFavoriteRequest

@Serializable
data class FileInfo(
  val name: String,
  val size: Long,
  val isDirectory: Boolean,
  val type: FileType,
  val lastModified: Long,
  val path: String
)

fun FileInfo.toAddFileToFavoriteRequest() = AddFileToFavoriteRequest(
  filename = name,
  filePath = path,
  isDirectory = isDirectory,
  fileType = type,
  lastModified = lastModified,
  fileSize = size
)
