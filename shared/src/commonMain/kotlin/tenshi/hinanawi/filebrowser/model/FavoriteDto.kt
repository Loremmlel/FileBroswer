package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
  val id: Long,
  val name: String,
  val createdAt: Long,
  val updatedAt: Long,
  val sortOrder: Int = 0,
  val files: List<FavoriteFileDto> = emptyList(),
)