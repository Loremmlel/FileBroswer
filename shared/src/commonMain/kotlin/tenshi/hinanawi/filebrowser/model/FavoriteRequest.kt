package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateFavoriteRequest(
  val parentId: Long? = null,
  val name: String,
  val sortOrder: Int = 0
)

@Serializable
data class UpdateFavoriteRequest(
  val name: String? = null,
  val sortOrder: Int? = null
)

@Serializable
data class MoveFavoriteRequest(
  val newParentId: Long? = null
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