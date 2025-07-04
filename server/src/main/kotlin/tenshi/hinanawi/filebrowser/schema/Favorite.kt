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
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.model.FavoriteFileDto
import tenshi.hinanawi.filebrowser.table.FavoriteFileTable
import tenshi.hinanawi.filebrowser.table.FavoriteTable

class Favorite(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<Favorite>(FavoriteTable)

  var parentId by FavoriteTable.parentId
  var name by FavoriteTable.name
  var createdAt by FavoriteTable.createdAt
  var updatedAt by FavoriteTable.updatedAt
  var sortOrder by FavoriteTable.sortOrder

  // 父收藏夹
  val parent by Favorite optionalReferencedOn FavoriteTable.parentId

  // 子收藏夹
  val children by Favorite optionalReferrersOn FavoriteTable.parentId

  // 收藏夹下的文件
  val files by FavoriteFile referrersOn FavoriteFileTable.favoriteId
}

class FavoriteService {
  /**
   * 创建收藏夹
   *
   * @param parentId 父收藏夹ID，默认为null表示创建顶级收藏夹
   * @param name 收藏夹名称
   * @param sortOrder 排序顺序，默认为0
   * @return 创建的收藏夹 [FavoriteDto]
   * @throws ServiceException 父收藏夹不存在
   */
  fun createFavorite(parentId: Long? = null, name: String, sortOrder: Int = 0) = transaction {
    if (parentId != null) {
      Favorite.findById(parentId) ?: throw ServiceException(ServiceMessage.FavoriteParentNotFound)
    }
    val favorite = Favorite.new {
      this.parentId = parentId?.let { EntityID(it, FavoriteTable) }
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
    if (favorite.children.count() > 0) {
      throw ServiceException(ServiceMessage.FavoriteContainsSub)
    }
    if (favorite.files.count() > 0) {
      throw ServiceException(ServiceMessage.FavoriteContainsFiles)
    }
    favorite.delete()
    true
  }

  /**
   * 获得收藏夹树，只包含收藏夹结构不包含文件内容
   *
   * @param parentId 父收藏夹ID，默认为null表示获取顶级收藏夹
   * @return 收藏夹树 [List]&[FavoriteDto]
   */
  fun getFavoriteTree(parentId: Long? = null): List<FavoriteDto> = transaction {
    val favorites = Favorite
      .find { FavoriteTable.parentId eq parentId }
      .orderBy(FavoriteTable.sortOrder to SortOrder.ASC)
    favorites.map { favorite ->
      favorite.toDto().copy(
        children = getFavoriteTree(favorite.id.value)
      )
    }
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
      children = favorite.children.orderBy(FavoriteTable.sortOrder to SortOrder.ASC).map { it.toDto() },
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

  /**
   * 移动收藏夹
   *
   * @param favoriteId 收藏夹ID
   * @param newParentId 新父收藏夹ID
   * @return 移动后的收藏夹 [FavoriteDto]
   * @throws ServiceException 收藏夹不存在或父收藏夹不存在或收藏夹不能移动到自身或其子收藏夹下
   */
  fun moveFavorite(favoriteId: Long, newParentId: Long?): FavoriteDto = transaction {
    val favorite = Favorite.findById(favoriteId) ?: throw ServiceException(ServiceMessage.FavoriteNotFound)
    if (newParentId != null) {
      Favorite.findById(newParentId) ?: throw ServiceException(ServiceMessage.FavoriteParentNotFound)
      if (isDescendant(favoriteId, newParentId)) {
        throw ServiceException(ServiceMessage.FavoriteCanNotMoveSelf)
      }
    }
    favorite.parentId = newParentId?.let { EntityID(it, FavoriteTable) }
    favorite.updatedAt = Clock.System.now()
    favorite.toDto()
  }

  /**
   * 判断一个收藏夹是否为另一个收藏夹的子孙收藏夹
   *
   * @param ancestorId 祖先收藏夹ID
   * @param descendantId 子孙收藏夹ID
   * @return 是否为子孙收藏夹 [Boolean]
   */
  private fun isDescendant(ancestorId: Long, descendantId: Long): Boolean = transaction {
    // bugfix: 修复收藏夹ID相同时能移动的问题。
    // 还得是单元测试
    if (ancestorId == descendantId) return@transaction true
    val descendant = Favorite.findById(descendantId) ?: return@transaction false
    var current = descendant.parent
    while (current != null) {
      if (current.id.value == ancestorId) {
        return@transaction true
      }
      current = current.parent
    }
    false
  }
}

fun Favorite.toDto(): FavoriteDto = FavoriteDto(
  id = id.value,
  parentId = parentId?.value,
  name = name,
  createdAt = createdAt.toEpochMilliseconds(),
  updatedAt = updatedAt.toEpochMilliseconds(),
  sortOrder = sortOrder
)