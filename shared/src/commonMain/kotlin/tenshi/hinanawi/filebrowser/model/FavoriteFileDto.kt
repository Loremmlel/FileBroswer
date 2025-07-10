package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteFileDto(
  val id: Long,
  val favoriteId: Long,
  val filename: String,
  val fileSize: Long,
  val fileType: FileType,
  val filePath: String,
  val lastModified: Long,
  val isDirectory: Boolean,
  val createdAt: Long
)

fun FavoriteFileDto.toFileInfo(): FileInfo = FileInfo(
  name = filename,
  size = fileSize,
  isDirectory = isDirectory,
  type = fileType,
  path = filePath,
  lastModified = lastModified
)