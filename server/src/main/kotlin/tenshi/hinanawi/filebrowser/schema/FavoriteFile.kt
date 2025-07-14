package tenshi.hinanawi.filebrowser.schema


import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto
import tenshi.hinanawi.filebrowser.table.FavoriteFileTable

class FavoriteFile(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<FavoriteFile>(FavoriteFileTable)

  var favoriteId by FavoriteFileTable.favoriteId
  var filename by FavoriteFileTable.filename
  var fileSize by FavoriteFileTable.fileSize
  var fileType by FavoriteFileTable.fileType
  var filePath by FavoriteFileTable.filePath
  var lastModified by FavoriteFileTable.lastModified
  var isDirectory by FavoriteFileTable.isDirectory
  var createdAt by FavoriteFileTable.createdAt
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