package tenshi.hinanawi.filebrowser.schema

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.model.FavoriteFileDto
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.table.FavoriteFiles
import tenshi.hinanawi.filebrowser.table.Favorites

class Favorite(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<Favorite>(Favorites)

  var parentId by Favorites.parentId
  var name by Favorites.name
  var isFolder by Favorites.isFolder
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
  fun getRootFavorites(): List<FavoriteDto> = transaction {
    Favorite.find { Favorites.parentId.isNull() }
      .orderBy(Favorites.sortOrder to SortOrder.ASC)
      .map { it.toDto() }
  }

  fun getChildFavorites(parentId: Long): List<FavoriteDto> = transaction {
    Favorite.find { Favorites.parentId eq parentId }
      .orderBy(Favorites.sortOrder to SortOrder.ASC)
      .map { it.toDto() }
  }

  fun createFolder(name: String, parentId: Long? = null): FavoriteDto = transaction {
    val favorite = Favorite.new {
      this.name = name
      this.parentId = parentId?.let { EntityID(it, Favorites) }
      this.isFolder = true
    }
    favorite.toDto()
  }

  fun addFileToFavorite(favoriteId: Long, fileInfo: FileInfo): FavoriteFileDto = transaction {
    val favoriteFile = FavoriteFile.new {
      this.favoriteId = EntityID(favoriteId, Favorites)
      this.filename = fileInfo.name
      this.fileSize = fileInfo.size
      this.fileType = fileInfo.type
      this.filePath = fileInfo.path
      this.lastModified = fileInfo.lastModified
      this.isDirectory = fileInfo.isDirectory
    }
    favoriteFile.toDto()
  }

  fun getFavoriteTree(rootId: Long? = null): List<FavoriteDto> = transaction {
    val favorites = Favorite
      .find { Favorites.parentId eq rootId }
      .orderBy(Favorites.sortOrder to SortOrder.ASC)
    favorites.map { favorite ->
      favorite.toDto().copy(
        children = getFavoriteTree(favorite.id.value),
        files = favorite.files.map { it.toDto() }
      )
    }
  }

  fun moveFavorite(favoriteId: Long, newParentId: Long? = null) = transaction {
    val favorite = Favorite.findById(favoriteId)
    favorite?.parentId = newParentId?.let { EntityID(it, Favorites) }
  }

  fun deleteFavorite(favoriteId: Long) = transaction {
    val favorite = Favorite.findById(favoriteId)
    favorite?.delete()
  }
}
fun Favorite.toDto(): FavoriteDto = FavoriteDto(
  id = id.value,
  parentId = parentId?.value,
  name = name,
  isFolder = isFolder,
  createdAt = createdAt.toEpochMilliseconds(),
  updatedAt = updatedAt.toEpochMilliseconds(),
  sortOrder = sortOrder
)