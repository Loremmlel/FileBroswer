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
import tenshi.hinanawi.filebrowser.route.random
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RandomEndpointTest : BaseEndpointTest() {
  private fun randomTestApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult = testApplication {
    install(ContentNegotiation) {
      json()
    }
    application {
      random()
    }
    block()
  }

  @Test
  fun `test successful scan with default type (video)`() = randomTestApplication {
    val testDir = File(baseDir, "movies").apply { mkdir() }
    val subDir = File(testDir, "series").apply { mkdir() }
    val video1 = File(testDir, "movie.mp4").apply { createNewFile() }
    val video2 = File(subDir, "episode1.mkv").apply { createNewFile() }
    File(testDir, "poster.jpg").apply { createNewFile() }
    File(subDir, "subs.srt").apply { createNewFile() }

    val response = client.get("/random?path=/movies")
    assertEquals(HttpStatusCode.OK, response.status)

    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(response.bodyAsText())
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
    val files = parsed.data
    assertNotNull(files)
    assertEquals(2, files.size)

    assertTrue { files.any { it.name == video1.name && it.type == FileType.Video } }
    assertTrue { files.any { it.name == video2.name && it.type == FileType.Video } }
  }

  @Test
  fun `test successful scan with explicit type (image)`() = randomTestApplication {
    val testDir = File(baseDir, "gallery").apply { mkdir() }
    val subDir = File(testDir, "vacation").apply { mkdir() }
    val image1 = File(testDir, "art.png").apply { createNewFile() }
    val image2 = File(subDir, "beach.jpg").apply { createNewFile() }
    File(testDir, "video.mp4").apply { createNewFile() }

    val response = client.get("/random?path=/gallery&type=Image")
    assertEquals(HttpStatusCode.OK, response.status)

    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(response.bodyAsText())
    assertEquals(200, parsed.code)
    val files = parsed.data
    assertNotNull(files)
    assertEquals(2, files.size)

    assertTrue { files.any { it.name == image1.name && it.type == FileType.Image } }
    assertTrue { files.any { it.name == image2.name && it.type == FileType.Image } }
  }

  @Test
  fun `test scan with no matching files returns empty list`() = randomTestApplication {
    val testDir = File(baseDir, "documents").apply { mkdir() }
    File(testDir, "report.pdf").apply { createNewFile() }
    File(testDir, "notes.txt").apply { createNewFile() }

    val response = client.get("/random?path=/documents&type=Video")
    assertEquals(HttpStatusCode.OK, response.status)

    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(response.bodyAsText())
    assertEquals(200, parsed.code)
    val files = parsed.data
    assertNotNull(files)
    assertTrue(files.isEmpty())
  }

  @Test
  fun `test scan on a file path instead of a directory`() = randomTestApplication {
    val testFile = File(baseDir, "single_file.mp4").apply { createNewFile() }

    val response = client.get("/random?path=/${testFile.name}&type=Video")
    assertEquals(HttpStatusCode.OK, response.status)

    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(response.bodyAsText())
    assertEquals(200, parsed.code)
    val files = parsed.data
    assertNotNull(files)
    assertTrue(files.isEmpty(), "扫描文件时要返回空列表")
  }

  @Test
  fun `test invalid type parameter falls back to video`() = randomTestApplication {
    val testDir = File(baseDir, "mixed").apply { mkdir() }
    val videoFile = File(testDir, "clip.mp4").apply { createNewFile() }
    File(testDir, "photo.jpg").apply { createNewFile() }

    val response = client.get("/random?path=/mixed&type=notARealType")
    assertEquals(HttpStatusCode.OK, response.status)

    val parsed = Json.decodeFromString<Response<List<FileInfo>>>(response.bodyAsText())
    val files = parsed.data
    assertNotNull(files)
    assertEquals(1, files.size)
    assertEquals(videoFile.name, files[0].name)
    assertEquals(FileType.Video, files[0].type)
  }

  @Test
  fun `test missing path parameter`() = randomTestApplication {
    val response = client.get("/random")
    assertEquals(HttpStatusCode.BadRequest, response.status)

    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
  }

  @Test
  fun `test invalid path`() = randomTestApplication {
    val response = client.get("/random?path=/non_existent_dir")
    assertEquals(HttpStatusCode.NotFound, response.status)

    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(404, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
  }

  @Test
  fun `test path traversal attempt`() = randomTestApplication {
    val response = client.get("/random?path=/../../etc")
    assertEquals(HttpStatusCode.Forbidden, response.status)

    val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
    assertEquals(403, parsed.code)
    assertEquals(Message.FilesForbidden, parsed.message)
  }
}