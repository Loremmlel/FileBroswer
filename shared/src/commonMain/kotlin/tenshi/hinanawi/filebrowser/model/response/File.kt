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

@Serializable
enum class FileType {
  Folder,
  Image,
  Video,
  Other;

  override fun toString(): String = this.name
}

fun String.parseFileType(): FileType? = when (this) {
  FileType.Folder.name -> FileType.Folder
  FileType.Image.name -> FileType.Image
  FileType.Video.name -> FileType.Video
  FileType.Other.name -> FileType.Other
  else -> null
}