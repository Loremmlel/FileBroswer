package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
  val id: Long,
  val parentId: Long? = null,
  val name: String,
  val isFolder: Boolean = false,
  val createdAt: Long,
  val updatedAt: Long,
  val sortOrder: Int = 0,
  val children: List<FavoriteDto> = emptyList(),
  val files: List<FavoriteFileDto> = emptyList()
)