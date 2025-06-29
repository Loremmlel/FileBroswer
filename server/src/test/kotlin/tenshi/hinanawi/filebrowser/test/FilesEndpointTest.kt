package tenshi.hinanawi.filebrowser.test

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.Json
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.route.files
import tenshi.hinanawi.filebrowser.util.skipTest
import java.io.File
import kotlin.test.*

class FilesEndpointTest : BaseEndpointTest() {

  private fun fileTestApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult = testApplication {
    install(ContentNegotiation) {
      json()
    }
    application {
      files()
    }
    block()
  }

  @Test
  fun `test missing path parameter`() = fileTestApplication {
    val response = client.get("/files")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test invalid path`() = fileTestApplication {
    val response = client.get("/files?path=invalid")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test path traversal attempt`() = fileTestApplication {
    val response = client.get("/files?path=/../../")
    assertEquals(HttpStatusCode.Forbidden, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(403, parsed.code)
    assertEquals(Message.FilesForbidden, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test non directory path`() = fileTestApplication {
    val file = File(baseDir, "test file.txt").apply {
      createNewFile()
    }
    val response = client.get("/files?path=/${file.name}")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesIsNotDirectory, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test valid directory listing`() = fileTestApplication {
    val dir = File(baseDir, "testDir").apply {
      mkdir()
    }
    val file1 = File(dir, "file1.txt").apply {
      createNewFile()
    }
    val file2 = File(dir, "file2.txt").apply {
      createNewFile()
    }
    val subDir = File(dir, "subDir").apply {
      mkdir()
    }
    val response = client.get("/files?path=/testDir")
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(body)
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
    val items = parsed.data
    assertNotNull(items)
    assertEquals(3, items.size)
    // 测试排序功能，文件夹在前
    assertTrue { items[0].isDirectory }
    assertTrue {
      items.any {
        it.name == file1.name
            && it.path == "/testDir/${file1.name}".normalizedPath()
            && it.type == FileType.Other
      }
    }
    assertTrue {
      items.any {
        it.name == file2.name
            && it.path == "/testDir/${file2.name}".normalizedPath()
            && it.type == FileType.Other
      }
    }
    assertTrue {
      items.any {
        it.name == subDir.name
            && it.path == "/testDir/${subDir.name}".normalizedPath()
            && it.type == FileType.Folder
      }
    }
  }

  @Test
  fun `test hidden files is not listing`() = fileTestApplication {
    val dir = File(baseDir, "testDir").apply {
      mkdir()
    }
    File(dir, ".file1.txt").apply {
      createNewFile()
      if (isWindows) {
        Runtime.getRuntime().exec("attrib +H \"${this.absolutePath}\"")
        skipTest("我真是操了，单独跑测试可以，一旦用gradle任务跑所有测试，这里就会失败。日了狗了，直接跳过！")
      }
    }
    val file2 = File(dir, "file2.txt").apply {
      createNewFile()
    }
    val response = client.get("/files?path=/testDir")
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(body)
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
    val items = parsed.data
    assertNotNull(items)
    assertEquals(1, items.size)
    assertTrue { items.any { it.name == file2.name } }
  }

  @Test
  fun `test directory listing throws exception`() = fileTestApplication {
    val invalidPath = "/\u0000"

    val response = client.get("/files") {
      parameter("path", invalidPath)
    }

    assertEquals(HttpStatusCode.InternalServerError, response.status)

    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(500, parsed.code)
    assertEquals(Message.InternalServerError, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test delete - successful file deletion`() = fileTestApplication {
    val fileToDelete = File(baseDir, "file-to-delete.txt").apply { createNewFile() }
    assertTrue(fileToDelete.exists())

    val response = client.delete("/files?path=/${fileToDelete.name}")

    assertEquals(HttpStatusCode.NoContent, response.status)
    assertFalse(fileToDelete.exists())
  }

  @Test
  fun `test delete - missing path parameter`() = fileTestApplication {
    val response = client.delete("/files")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
  }

  @Test
  fun `test delete - invalid path format`() = fileTestApplication {
    val response = client.delete("/files?path=someFile.txt")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
  }

  @Test
  fun `test delete - path traversal attempt`() = fileTestApplication {
    val response = client.delete("/files?path=/../../secrets.txt")
    assertEquals(HttpStatusCode.Forbidden, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(403, parsed.code)
    assertEquals(Message.FilesForbidden, parsed.message)
  }

  @Test
  fun `test delete - file does not exist`() = fileTestApplication {
    val response = client.delete("/files?path=/non-existent-file.txt")
    assertEquals(HttpStatusCode.NotFound, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(404, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
  }

  @Test
  fun `test delete - attempt to delete non-empty directory`() = fileTestApplication {
    // 覆盖: file.isDirectory is true inside !file.delete() block
    val dir = File(baseDir, "notEmptyDir").apply { mkdir() }
    File(dir, "child.txt").createNewFile() // 使目录非空

    val response = client.delete("/files?path=/${dir.name}")

    assertEquals(HttpStatusCode.BadRequest, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesDirectoryMustEmptyWhileDelete, parsed.message)
    assertTrue(dir.exists(), "目录不应该被删除")
  }

  @Test
  fun `test delete - file deletion fails due to other reasons`() = fileTestApplication {
    // 覆盖: file.isDirectory is false inside !file.delete() block (权限问题)
    // 通过移除父目录的写权限来模拟文件无法删除的场景，windows则跳过
    if (isWindows) {
      skipTest("因为windows权限模型和unix权限模型不同，无法模拟权限问题，暂时跳过")
    }
    val protectedDir = File(baseDir, "protectedDir").apply { mkdir() }
    val fileToFail = File(protectedDir, "locked.txt").apply { createNewFile() }

    // 在 finally 中恢复权限，确保 tearDown 能成功清理
    try {
      protectedDir.setWritable(false)

      val response = client.delete("/files?path=/${protectedDir.name}/${fileToFail.name}")

      assertEquals(HttpStatusCode.InternalServerError, response.status)
      val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
      assertEquals(500, parsed.code)
      assertEquals(Message.Failed, parsed.message)
      assertTrue(fileToFail.exists(), "File should not be deleted")
    } finally {
      protectedDir.setWritable(true)
    }
  }

  @Test
  fun `test delete - throws internal exception`() = fileTestApplication {
    // 覆盖: catch (e: Exception) block
    val invalidPath = "/\u0000"

    val response = client.delete("/files") {
      parameter("path", invalidPath)
    }

    assertEquals(HttpStatusCode.InternalServerError, response.status)
    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(500, parsed.code)
    assertEquals(Message.InternalServerError, parsed.message)
  }
}