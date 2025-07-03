package tenshi.hinanawi.filebrowser.table

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object FavoriteTable : LongIdTable("favorites") {
  val parentId = reference(
    "parent_id",
    FavoriteTable,
    onDelete = ReferenceOption.RESTRICT,
    onUpdate = ReferenceOption.CASCADE
  ).nullable()
  val name = varchar("name", 255)
  val createdAt = timestamp("created_at").default(Clock.System.now())
  val updatedAt = timestamp("updated_at").default(Clock.System.now())
  val sortOrder = integer("sort_order").default(0)

  init {
    uniqueIndex(parentId, name)
  }
}