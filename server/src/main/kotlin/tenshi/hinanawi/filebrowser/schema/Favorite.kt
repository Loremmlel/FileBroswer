package tenshi.hinanawi.filebrowser.schema

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.table.FavoriteFiles
import tenshi.hinanawi.filebrowser.table.Favorites

class Favorite(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<Favorite>(Favorites)

  var parentId by Favorites.parentId
  var name by Favorites.name
  var createdAt by Favorites.createdAt
  var updatedAt by Favorites.updatedAt
  var sortOrder by Favorites.sortOrder

  // 父收藏夹
  val parent by Favorite optionalReferencedOn Favorites.parentId

  // 子收藏夹
  val children by Favorite optionalReferrersOn Favorites.parentId

  // 收藏夹下的文件
  val files by FavoriteFile referrersOn FavoriteFiles.favoriteId
}

class FavoriteService {
}
fun Favorite.toDto(): FavoriteDto = FavoriteDto(
  id = id.value,
  parentId = parentId?.value,
  name = name,
  createdAt = createdAt.toEpochMilliseconds(),
  updatedAt = updatedAt.toEpochMilliseconds(),
  sortOrder = sortOrder
)