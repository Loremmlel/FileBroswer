package tenshi.hinanawi.filebrowser.table

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import tenshi.hinanawi.filebrowser.model.FileType

object FavoriteFiles : LongIdTable("favorite_files") {
  val favoriteId = reference(
    "favorite_id",
    Favorites,
    onDelete = ReferenceOption.CASCADE
  )
  val filename = varchar("filename", 255)
  val fileSize = long("file_size")
  val fileType = enumerationByName("file_type", 50, FileType::class)
  val filePath = text("file_path")
  val lastModified = long("last_modified")
  val isDirectory = bool("is_directory").default(false)
  val createdAt = timestamp("created_at").default(Clock.System.now())

  init {
    uniqueIndex(favoriteId, filePath)
  }
}