package tenshi.hinanawi.filebrowser.schema


import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import tenshi.hinanawi.filebrowser.model.FavoriteFileDto
import tenshi.hinanawi.filebrowser.table.FavoriteFiles

class FavoriteFile(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<FavoriteFile>(FavoriteFiles)

  var favoriteId by FavoriteFiles.favoriteId
  var filename by FavoriteFiles.filename
  var fileSize by FavoriteFiles.fileSize
  var fileType by FavoriteFiles.fileType
  var filePath by FavoriteFiles.filePath
  var lastModified by FavoriteFiles.lastModified
  var isDirectory by FavoriteFiles.isDirectory
  var createdAt by FavoriteFiles.createdAt

  // 关联的收藏夹
  val favorite by Favorite referrersOn FavoriteFiles.favoriteId
}

fun FavoriteFile.toDto(): FavoriteFileDto = FavoriteFileDto(
  id = id.value,
  favoriteId = favoriteId.value,
  filename = filename,
  fileSize = fileSize,
  fileType = fileType,
  filePath = filePath,
  lastModified = lastModified,
  isDirectory = isDirectory,
  createdAt = createdAt.toEpochMilliseconds()
)