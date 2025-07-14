package tenshi.hinanawi.filebrowser.schema

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import tenshi.hinanawi.filebrowser.exception.ServiceException
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto
import tenshi.hinanawi.filebrowser.table.FavoriteFileTable
import tenshi.hinanawi.filebrowser.table.FavoriteTable

class Favorite(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<Favorite>(FavoriteTable)

  var name by FavoriteTable.name
  var createdAt by FavoriteTable.createdAt
  var updatedAt by FavoriteTable.updatedAt
  var sortOrder by FavoriteTable.sortOrder

  // 收藏夹下的文件
  val files by FavoriteFile referrersOn FavoriteFileTable.favoriteId
}

class FavoriteService {
  /**
   * 获取收藏夹列表
   * @return 收藏夹列表 [List] [FavoriteDto]
   */
  fun getFavorites() = transaction {
    Favorite.all().map { it.toDto() }
  }

  /**
   * 创建收藏夹
   *
   * @param name 收藏夹名称
   * @param sortOrder 排序顺序，默认为0
   * @return 创建的收藏夹 [FavoriteDto]
   * @throws ServiceException 父收藏夹不存在
   */
  fun createFavorite(name: String, sortOrder: Int = 0) = transaction {
    val favorite = Favorite.new {
      this.name = name
      this.sortOrder = sortOrder
      this.updatedAt = Clock.System.now()
    }
    favorite.toDto()
  }

  /**
   * 删除收藏夹
   *
   * @param favoriteId 收藏夹ID
   * @return 是否删除成功 [Boolean]
   * @throws ServiceException 收藏夹不存在或包含子收藏夹/文件
   */
  fun deleteFavorite(favoriteId: Long): Boolean = transaction {
    val favorite = Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    if (favorite.files.count() > 0) {
      throw ServiceException(ServiceMessage.FavoriteContainsFiles)
    }
    favorite.delete()
    true
  }

  /**
   * 获得单个收藏夹详情
   *
   * @param favoriteId 收藏夹ID
   * @return 收藏夹详情 [FavoriteDto]
   * @throws ServiceException 收藏夹不存在
   */
  fun getFavoriteDetail(favoriteId: Long): FavoriteDto = transaction {
    val favorite = Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    favorite.toDto().copy(
      files = favorite.files.orderBy(FavoriteFileTable.createdAt to SortOrder.DESC).map { it.toDto() }
    )
  }

  /**
   * 更新收藏夹
   *
   * @param favoriteId 收藏夹ID
   * @param name 收藏夹名称
   * @param sortOrder 排序顺序
   * @return 更新后的收藏夹 [FavoriteDto]
   * @throws ServiceException 收藏夹不存在
   */
  fun updateFavorite(favoriteId: Long, name: String?, sortOrder: Int?): FavoriteDto = transaction {
    val favorite = Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    name?.let { favorite.name = it }
    sortOrder?.let { favorite.sortOrder = it }
    favorite.updatedAt = Clock.System.now()
    favorite.toDto()
  }

  /**
   * 将文件添加到收藏夹
   *
   * @param favoriteId 收藏夹ID
   * @param file 文件信息
   * @return 添加的文件信息 [FavoriteFileDto]
   * @throws ServiceException 收藏夹不存在
   */
  fun addFileToFavorite(favoriteId: Long, file: FavoriteFileDto): FavoriteFileDto = transaction {
    Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    val favoriteFile = FavoriteFile.new {
      this.favoriteId = EntityID(favoriteId, FavoriteFileTable)
      this.filename = file.filename
      this.fileSize = file.fileSize
      this.fileType = file.fileType
      this.filePath = file.filePath
      this.lastModified = file.lastModified
      this.isDirectory = file.isDirectory
      this.createdAt = Clock.System.now() // bugfix: 单元测试发现错误：显然应该用服务器实际创建的时间戳，而不是DTO给的！
    }
    favoriteFile.toDto()
  }

  /**
   * 将文件列表添加到收藏夹
   *
   * TODO: 可以考虑升级为批量插入
   *
   * @param favoriteId 收藏夹ID
   * @param files 文件信息列表
   * @return 添加的文件信息列表 [List]&[FavoriteFileDto]
   * @throws ServiceException 收藏夹不存在
   */
  fun addFilesToFavorite(favoriteId: Long, files: List<FavoriteFileDto>): List<FavoriteFileDto> = transaction {
    Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    files.map {
      addFileToFavorite(favoriteId, it)
    }
  }

  /**
   * 删除收藏文件
   *
   * @param favoriteFileId 收藏文件ID
   * @return 是否删除成功 [Boolean]
   * @throws ServiceException 收藏文件不存在
   */
  fun removeFavoriteFile(favoriteFileId: Long): Boolean = transaction {
    val favoriteFile =
      FavoriteFile.findById(favoriteFileId) ?: throw ServiceException(ServiceMessage.FavoriteFileNotFound)
    favoriteFile.delete()
    true
  }

  /**
   * 批量删除收藏文件
   *
   * @param favoriteFileIds 收藏文件ID列表
   * @return 删除的数量 [Int]
   */
  fun removeFavoriteFiles(favoriteFileIds: List<Long>): Int = transaction {
    val deleteCount = FavoriteFile.find { FavoriteFileTable.id inList favoriteFileIds }.count()
    FavoriteFileTable.deleteWhere { FavoriteFileTable.id inList favoriteFileIds }
    deleteCount.toInt()
  }

  fun getAllFavoriteFiles(): List<FavoriteFileDto> = transaction {
    FavoriteFile.all().map { it.toDto() }
  }
}

fun Favorite.toDto(): FavoriteDto = FavoriteDto(
  id = id.value,
  name = name,
  createdAt = createdAt.toEpochMilliseconds(),
  updatedAt = updatedAt.toEpochMilliseconds(),
  sortOrder = sortOrder,
  // 忘了加files导致一直为空...
  files = files.toList().map { it.toDto() }
)