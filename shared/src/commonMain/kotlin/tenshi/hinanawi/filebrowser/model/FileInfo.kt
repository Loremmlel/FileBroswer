package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
  val name: String,
  val size: Long,
  val isDirectory: Boolean,
  val type: FileType,
  val lastModified: Long,
  val path: String
)
