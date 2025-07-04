package tenshi.hinanawi.filebrowser.service

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.transaction
import tenshi.hinanawi.filebrowser.exception.ServiceException
import tenshi.hinanawi.filebrowser.model.FavoriteFileDto
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.schema.Favorite
import tenshi.hinanawi.filebrowser.schema.FavoriteService
import tenshi.hinanawi.filebrowser.schema.ServiceMessage
import tenshi.hinanawi.filebrowser.schema.toDto
import kotlin.test.*

class FavoriteServiceTest : BaseServiceTest() {

  private val favoriteService = FavoriteService()

  @Test
  fun `test createFavorite with null parentId creates top level favorite`() {
    val favorite = favoriteService.createFavorite(
      parentId = null,
      name = "顶级收藏夹",
      sortOrder = 1
    )

    assertNotNull(favorite)
    assertEquals("顶级收藏夹", favorite.name)
    assertNull(favorite.parentId)
    assertEquals(1, favorite.sortOrder)
    assertTrue(favorite.id > 0)
    assertTrue(favorite.createdAt > 0)
    assertTrue(favorite.updatedAt > 0)
  }

  @Test
  fun `test createFavorite with valid parentId creates sub favorite`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    val child = favoriteService.createFavorite(
      parentId = parent.id,
      name = "子收藏夹",
      sortOrder = 2
    )

    assertNotNull(child)
    assertEquals("子收藏夹", child.name)
    assertEquals(parent.id, child.parentId)
    assertEquals(2, child.sortOrder)
  }

  @Test
  fun `test createFavorite with default sortOrder`() {
    val favorite = favoriteService.createFavorite(name = "默认排序收藏夹")

    assertEquals(0, favorite.sortOrder)
  }

  @Test
  fun `test createFavorite with invalid parentId throws exception`() {
    val exception = assertFailsWith<ServiceException> {
      favoriteService.createFavorite(
        parentId = 999L,
        name = "无效父收藏夹"
      )
    }
    assertEquals(ServiceMessage.FavoriteParentNotFound, exception.serviceMessage)
  }

  @Test
  fun `test deleteFavorite successfully deletes empty favorite`() {
    val favorite = favoriteService.createFavorite(name = "待删除收藏夹")
    val result = favoriteService.deleteFavorite(favorite.id)

    assertTrue(result)

    // 验证收藏夹已被删除
    val exception = assertFailsWith<ServiceException> {
      favoriteService.getFavoriteDetail(favorite.id)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test deleteFavorite with non-existent id throws exception`() {
    val exception = assertFailsWith<ServiceException> {
      favoriteService.deleteFavorite(999L)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test deleteFavorite with sub favorites throws exception`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹")

    val exception = assertFailsWith<ServiceException> {
      favoriteService.deleteFavorite(parent.id)
    }
    assertEquals(ServiceMessage.FavoriteContainsSub, exception.serviceMessage)
  }

  @Test
  fun `test deleteFavorite with files throws exception`() {
    val favorite = favoriteService.createFavorite(name = "包含文件的收藏夹")
    val fileDto = createTestFavoriteFileDto(favorite.id)
    favoriteService.addFileToFavorite(favorite.id, fileDto)

    val exception = assertFailsWith<ServiceException> {
      favoriteService.deleteFavorite(favorite.id)
    }
    assertEquals(ServiceMessage.FavoriteContainsFiles, exception.serviceMessage)
  }

  @Test
  fun `test getFavoriteTree returns empty list when no favorites exist`() {
    val tree = favoriteService.getFavoriteTree()
    assertTrue(tree.isEmpty())
  }

  @Test
  fun `test getFavoriteTree returns top level favorites only`() {
    val favorite1 = favoriteService.createFavorite(name = "收藏夹1", sortOrder = 2)
    val favorite2 = favoriteService.createFavorite(name = "收藏夹2", sortOrder = 1)
    favoriteService.createFavorite(parentId = favorite1.id, name = "子收藏夹")

    val tree = favoriteService.getFavoriteTree()

    assertEquals(2, tree.size)

    assertEquals(favorite2.name, tree[0].name)
    assertEquals(favorite1.name, tree[1].name)
    // 验证不包含收藏文件
    assertTrue(tree[0].files.isEmpty())
    assertTrue(tree[1].files.isEmpty())
  }

  @Test
  fun `test getFavoriteTree with parentId returns sub favorites`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    val child1 = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹1", sortOrder = 2)
    val child2 = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹2", sortOrder = 1)

    val tree = favoriteService.getFavoriteTree(parent.id)

    assertEquals(2, tree.size)
    assertEquals(child2.name, tree[0].name)
    assertEquals(child1.name, tree[1].name)
  }

  @Test
  fun `test getFavoriteTree returns nested structure`() {
    val root = favoriteService.createFavorite(name = "根收藏夹")
    val child = favoriteService.createFavorite(parentId = root.id, name = "子收藏夹")
    val grandchild = favoriteService.createFavorite(parentId = child.id, name = "孙收藏夹")

    val tree = favoriteService.getFavoriteTree()

    assertEquals(1, tree.size)
    assertEquals(root.name, tree[0].name)
    assertEquals(1, tree[0].children.size)
    assertEquals(child.name, tree[0].children[0].name)
    assertEquals(1, tree[0].children[0].children.size)
    assertEquals(grandchild.name, tree[0].children[0].children[0].name)
  }

  @Test
  fun `test getFavoriteDetail returns favorite with children and files`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    val child1 = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹1", sortOrder = 2)
    val child2 = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹2", sortOrder = 1)

    val fileDto1 = createTestFavoriteFileDto(parent.id, "文件1.mp4")
    val fileDto2 = createTestFavoriteFileDto(parent.id, "文件2.jpg")
    favoriteService.addFileToFavorite(parent.id, fileDto1)
    // 稍微延迟以确保创建时间不同
    Thread.sleep(1)
    favoriteService.addFileToFavorite(parent.id, fileDto2)

    val detail = favoriteService.getFavoriteDetail(parent.id)

    assertEquals(parent.name, detail.name)
    assertEquals(2, detail.children.size)
    // 验证子收藏夹按 sortOrder 排序
    assertEquals(child2.name, detail.children[0].name)
    assertEquals(child1.name, detail.children[1].name)

    assertEquals(2, detail.files.size)
    // 验证文件按创建时间倒序排序
    assertEquals(fileDto2.filename, detail.files[0].filename)
    assertEquals(fileDto1.filename, detail.files[1].filename)
  }

  @Test
  fun `test getFavoriteDetail with non-existent id throws exception`() {
    val exception = assertFailsWith<ServiceException> {
      favoriteService.getFavoriteDetail(999L)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test updateFavorite updates name only`() {
    val favorite = favoriteService.createFavorite(name = "原名称", sortOrder = 5)
    val originalUpdatedAt = favorite.updatedAt

    // 稍微延迟以确保更新时间不同
    Thread.sleep(1)
    val updated = favoriteService.updateFavorite(favorite.id, name = "新名称", sortOrder = null)

    assertEquals("新名称", updated.name)
    assertEquals(5, updated.sortOrder) // sortOrder 保持不变
    assertTrue(updated.updatedAt > originalUpdatedAt)
  }

  @Test
  fun `test updateFavorite updates sortOrder only`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹", sortOrder = 5)
    val updated = favoriteService.updateFavorite(favorite.id, name = null, sortOrder = 10)

    assertEquals("收藏夹", updated.name) // name 保持不变
    assertEquals(10, updated.sortOrder)
  }

  @Test
  fun `test updateFavorite updates both name and sortOrder`() {
    val favorite = favoriteService.createFavorite(name = "原名称", sortOrder = 5)
    val updated = favoriteService.updateFavorite(favorite.id, name = "新名称", sortOrder = 10)

    assertEquals("新名称", updated.name)
    assertEquals(10, updated.sortOrder)
  }

  @Test
  fun `test updateFavorite with non-existent id throws exception`() {
    val exception = assertFailsWith<ServiceException> {
      favoriteService.updateFavorite(999L, name = "新名称", sortOrder = null)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test addFileToFavorite successfully adds file`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")
    val fileDto = createTestFavoriteFileDto(favorite.id)

    val result = favoriteService.addFileToFavorite(favorite.id, fileDto)

    assertNotNull(result)
    assertTrue(result.id > 0)
    assertEquals(favorite.id, result.favoriteId)
    assertEquals("test.mp4", result.filename)
    assertEquals(1024L, result.fileSize)
    assertEquals(FileType.Video, result.fileType)
    assertEquals("/path/to/test.mp4", result.filePath)
    assertEquals(1234567890L, result.lastModified)
    assertFalse(result.isDirectory)
    assertTrue(result.createdAt > 0)
  }

  @Test
  fun `test addFileToFavorite with non-existent favorite throws exception`() {
    val fileDto = createTestFavoriteFileDto(999L)

    val exception = assertFailsWith<ServiceException> {
      favoriteService.addFileToFavorite(999L, fileDto)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test addFilesToFavorite successfully adds multiple files`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")
    val files = listOf(
      createTestFavoriteFileDto(favorite.id, "文件1.mp4"),
      createTestFavoriteFileDto(favorite.id, "文件2.jpg"),
      createTestFavoriteFileDto(favorite.id, "文件3.png")
    )

    val results = favoriteService.addFilesToFavorite(favorite.id, files)

    assertEquals(3, results.size)
    assertEquals(files[0].filename, results[0].filename)
    assertEquals(files[1].filename, results[1].filename)
    assertEquals(files[2].filename, results[2].filename)
    results.forEach { result ->
      assertTrue(result.id > 0)
      assertEquals(favorite.id, result.favoriteId)
    }
  }

  @Test
  fun `test addFilesToFavorite with empty list returns empty list`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")
    val results = favoriteService.addFilesToFavorite(favorite.id, emptyList())

    assertTrue(results.isEmpty())
  }

  @Test
  fun `test addFilesToFavorite with non-existent favorite throws exception`() {
    val files = listOf(createTestFavoriteFileDto(999L))

    val exception = assertFailsWith<ServiceException> {
      favoriteService.addFilesToFavorite(999L, files)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test removeFavoriteFile successfully removes file`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")
    val fileDto = createTestFavoriteFileDto(favorite.id)
    val addedFile = favoriteService.addFileToFavorite(favorite.id, fileDto)

    val result = favoriteService.removeFavoriteFile(addedFile.id)

    assertTrue(result)

    // 验证文件已被删除
    val detail = favoriteService.getFavoriteDetail(favorite.id)
    assertTrue(detail.files.isEmpty())
  }

  @Test
  fun `test removeFavoriteFile with non-existent id throws exception`() {
    val exception = assertFailsWith<ServiceException> {
      favoriteService.removeFavoriteFile(999L)
    }
    assertEquals(ServiceMessage.FavoriteFileNotFound, exception.serviceMessage)
  }

  @Test
  fun `test removeFavoriteFiles successfully removes multiple files`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")
    val file1 = favoriteService.addFileToFavorite(favorite.id, createTestFavoriteFileDto(favorite.id, "文件1.mp4"))
    val file2 = favoriteService.addFileToFavorite(favorite.id, createTestFavoriteFileDto(favorite.id, "文件2.jpg"))
    val file3 = favoriteService.addFileToFavorite(favorite.id, createTestFavoriteFileDto(favorite.id, "文件3.png"))

    val deleteCount = favoriteService.removeFavoriteFiles(listOf(file1.id, file2.id))

    assertEquals(2, deleteCount)

    // 验证只剩下一个文件
    val detail = favoriteService.getFavoriteDetail(favorite.id)
    assertEquals(1, detail.files.size)
    assertEquals(file3.filename, detail.files[0].filename)
  }

  @Test
  fun `test removeFavoriteFiles with empty list returns zero`() {
    val deleteCount = favoriteService.removeFavoriteFiles(emptyList())
    assertEquals(0, deleteCount)
  }

  @Test
  fun `test removeFavoriteFiles with non-existent ids returns zero`() {
    val deleteCount = favoriteService.removeFavoriteFiles(listOf(999L, 998L))
    assertEquals(0, deleteCount)
  }

  @Test
  fun `test moveFavorite to null parent moves to top level`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    val child = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹")

    val moved = favoriteService.moveFavorite(child.id, null)

    assertEquals(child.name, moved.name)
    assertNull(moved.parentId)
  }

  @Test
  fun `test moveFavorite to valid parent`() {
    val parent1 = favoriteService.createFavorite(name = "父收藏夹1")
    val parent2 = favoriteService.createFavorite(name = "父收藏夹2")
    val child = favoriteService.createFavorite(parentId = parent1.id, name = "子收藏夹")

    val moved = favoriteService.moveFavorite(child.id, parent2.id)

    assertEquals(child.name, moved.name)
    assertEquals(parent2.id, moved.parentId)
  }

  @Test
  fun `test moveFavorite with non-existent favorite throws exception`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")

    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(999L, parent.id)
    }
    assertEquals(ServiceMessage.FavoriteNotFound, exception.serviceMessage)
  }

  @Test
  fun `test moveFavorite with non-existent parent throws exception`() {
    val child = favoriteService.createFavorite(name = "子收藏夹")

    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(child.id, 999L)
    }
    assertEquals(ServiceMessage.FavoriteParentNotFound, exception.serviceMessage)
  }

  @Test
  fun `test moveFavorite to itself throws exception`() {
    val favorite = favoriteService.createFavorite(name = "收藏夹")

    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(favorite.id, favorite.id)
    }
    assertEquals(ServiceMessage.FavoriteCanNotMoveSelf, exception.serviceMessage)
  }

  @Test
  fun `test moveFavorite to descendant throws exception`() {
    val grandparent = favoriteService.createFavorite(name = "祖父收藏夹")
    val parent = favoriteService.createFavorite(parentId = grandparent.id, name = "父收藏夹")
    val child = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹")

    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(grandparent.id, child.id)
    }
    assertEquals(ServiceMessage.FavoriteCanNotMoveSelf, exception.serviceMessage)
  }

  @Test
  fun `test isDescendant returns true for direct child`() {
    val parent = favoriteService.createFavorite(name = "父收藏夹")
    val child = favoriteService.createFavorite(parentId = parent.id, name = "子收藏夹")

    // 通过尝试移动来间接测试 isDescendant 方法
    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(parent.id, child.id)
    }
    assertEquals(ServiceMessage.FavoriteCanNotMoveSelf, exception.serviceMessage)
  }

  @Test
  fun `test isDescendant returns true for deep descendant`() {
    val root = favoriteService.createFavorite(name = "根收藏夹")
    val level1 = favoriteService.createFavorite(parentId = root.id, name = "一级收藏夹")
    val level2 = favoriteService.createFavorite(parentId = level1.id, name = "二级收藏夹")
    val level3 = favoriteService.createFavorite(parentId = level2.id, name = "三级收藏夹")

    // 通过尝试移动来间接测试 isDescendant 方法
    val exception = assertFailsWith<ServiceException> {
      favoriteService.moveFavorite(root.id, level3.id)
    }
    assertEquals(ServiceMessage.FavoriteCanNotMoveSelf, exception.serviceMessage)
  }

  @Test
  fun `test isDescendant returns false for non-descendant`() {
    val favorite1 = favoriteService.createFavorite(name = "收藏夹1")
    val favorite2 = favoriteService.createFavorite(name = "收藏夹2")

    // 这应该成功，因为它们不是祖先-后代关系
    val moved = favoriteService.moveFavorite(favorite1.id, favorite2.id)
    assertEquals(favorite2.id, moved.parentId)
  }

  @Test
  fun `test toDto extension function converts correctly`() {
    val favorite = favoriteService.createFavorite(
      name = "测试收藏夹",
      sortOrder = 5
    )

    // 通过数据库直接获取实体来测试 toDto 扩展函数
    transaction {
      val entity = Favorite.findById(favorite.id)!!
      val dto = entity.toDto()

      assertEquals(favorite.id, dto.id)
      assertEquals(favorite.name, dto.name)
      assertEquals(favorite.parentId, dto.parentId)
      assertEquals(favorite.sortOrder, dto.sortOrder)
      assertEquals(favorite.createdAt, dto.createdAt)
      assertEquals(favorite.updatedAt, dto.updatedAt)
      assertTrue(dto.children.isEmpty()) // toDto() 不包含关联数据
      assertTrue(dto.files.isEmpty())
    }
  }

  private fun createTestFavoriteFileDto(
    favoriteId: Long,
    filename: String = "test.mp4"
  ): FavoriteFileDto {
    return FavoriteFileDto(
      id = 0, // 临时ID，会在创建时分配
      favoriteId = favoriteId,
      filename = filename,
      fileSize = 1024L,
      fileType = when {
        filename.endsWith(".mp4") -> FileType.Video
        filename.endsWith(".jpg") || filename.endsWith(".png") -> FileType.Image
        else -> FileType.Other
      },
      filePath = "/path/to/$filename",
      lastModified = 1234567890L,
      isDirectory = false,
      createdAt = Clock.System.now().toEpochMilliseconds()
    )
  }
}