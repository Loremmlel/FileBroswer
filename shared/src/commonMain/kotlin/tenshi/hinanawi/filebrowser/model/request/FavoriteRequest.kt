package tenshi.hinanawi.filebrowser.model.request

import kotlinx.serialization.Serializable
import tenshi.hinanawi.filebrowser.model.response.FileType

@Serializable
data class CreateFavoriteRequest(
  val name: String,
  val sortOrder: Int = 0
)

@Serializable
data class UpdateFavoriteRequest(
  val name: String? = null,
  val sortOrder: Int? = null
)

@Serializable
data class AddFileToFavoriteRequest(
  val filename: String,
  val fileSize: Long,
  val fileType: FileType,
  val filePath: String,
  val lastModified: Long,
  val isDirectory: Boolean
)

@Serializable
data class AddFilesToFavoriteRequest(
  val files: List<AddFileToFavoriteRequest>
)

@Serializable
data class RemoveFavoriteFilesRequest(
  val favoriteFileIds: List<Long>
)