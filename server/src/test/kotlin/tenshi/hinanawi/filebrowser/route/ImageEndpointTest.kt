package tenshi.hinanawi.filebrowser.route

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.readRawBytes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.Json
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import java.io.File
import kotlin.test.*

class ImageEndpointTest : BaseEndpointTest() {
  private fun imageTestApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult = testApplication {
    install(ContentNegotiation) {
      json()
    }
    application {
      routing {
        image()
      }
    }
    block()
  }

  @Test
  fun `test request non-image file - text file`() = imageTestApplication {
    val testFile = File(baseDir, "test.txt").apply {
      createNewFile()
      writeText("This is a text file")
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.ImageIsNotImage, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test request non-image file - video file`() = imageTestApplication {
    val testFile = File(baseDir, "test.mp4").apply {
      createNewFile()
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.ImageIsNotImage, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test request directory instead of file`() = imageTestApplication {
    val testDir = File(baseDir, "testDir").apply {
      mkdir()
    }

    val response = client.get("/image?path=/${testDir.name}")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.ImageIsNotImage, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test successful PNG image request`() = imageTestApplication {
    val pngData = byteArrayOf(
      0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
      0x00, 0x00, 0x00, 0x0D,
      0x49, 0x48, 0x44, 0x52
    )
    val testFile = File(baseDir, "test.png").apply {
      createNewFile()
      writeBytes(pngData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/png", contentType)

    // 验证Content-Length头
    val contentLength = response.headers[HttpHeaders.ContentLength]
    assertNotNull(contentLength)
    assertEquals(testFile.length().toString(), contentLength)

    // 验证响应内容
    val responseBytes = response.readRawBytes()
    assertTrue(pngData.contentEquals(responseBytes))
  }

  @Test
  fun `test successful JPEG image request`() = imageTestApplication {
    val jpegData = byteArrayOf(
      0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
      0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    )
    val testFile = File(baseDir, "test.jpg").apply {
      createNewFile()
      writeBytes(jpegData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/jpeg", contentType)

    // 验证Content-Length头
    val contentLength = response.headers[HttpHeaders.ContentLength]
    assertNotNull(contentLength)
    assertEquals(testFile.length().toString(), contentLength)

    // 验证响应内容
    val responseBytes = response.readBytes()
    assertTrue(jpegData.contentEquals(responseBytes))
  }

  @Test
  fun `test successful GIF image request`() = imageTestApplication {
    val gifData = byteArrayOf(
      0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
      0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00
    )
    val testFile = File(baseDir, "test.gif").apply {
      createNewFile()
      writeBytes(gifData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/gif", contentType)

    // 验证响应内容
    val responseBytes = response.readBytes()
    assertTrue(gifData.contentEquals(responseBytes))
  }

  @Test
  fun `test successful WebP image request`() = imageTestApplication {
    val webpData = byteArrayOf(
      0x52, 0x49, 0x46, 0x46,
      0x0C, 0x00, 0x00, 0x00,
      0x57, 0x45, 0x42, 0x50
    )
    val testFile = File(baseDir, "test.webp").apply {
      createNewFile()
      writeBytes(webpData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/webp", contentType)
  }

  @Test
  fun `test empty image file`() = imageTestApplication {
    val testFile = File(baseDir, "empty.png").apply {
      createNewFile()
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/png", contentType)

    // 验证Content-Length头
    val contentLength = response.headers[HttpHeaders.ContentLength]
    assertNotNull(contentLength)
    assertEquals("0", contentLength)

    // 验证响应内容为空
    val responseBytes = response.readRawBytes()
    assertEquals(0, responseBytes.size)
  }

  @Test
  fun `test large image file`() = imageTestApplication {
    val largeImageData = ByteArray(50000) { (it % 256).toByte() } // 50KB 数据
    // 添加PNG文件头
    val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    val fullData = pngHeader + largeImageData

    val testFile = File(baseDir, "large.png").apply {
      createNewFile()
      writeBytes(fullData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/png", contentType)

    // 验证Content-Length头
    val contentLength = response.headers[HttpHeaders.ContentLength]
    assertNotNull(contentLength)
    assertEquals(testFile.length().toString(), contentLength)

    // 验证响应内容大小
    val responseBytes = response.readRawBytes()
    assertEquals(fullData.size, responseBytes.size)
  }

  @Test
  fun `test image with special characters in filename`() = imageTestApplication {
    val specialFileName = "测试图片 (1) [copy].png"
    val pngData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    File(baseDir, specialFileName).apply {
      createNewFile()
      writeBytes(pngData)
    }

    val response = client.get("/image?path=/${specialFileName}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/png", contentType)

    // 验证响应内容
    val responseBytes = response.readRawBytes()
    assertTrue(pngData.contentEquals(responseBytes))
  }

  @Test
  fun `test case insensitive file extension`() = imageTestApplication {
    val pngData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    // 测试大写扩展名
    val testFile = File(baseDir, "test.PNG").apply {
      createNewFile()
      writeBytes(pngData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/png", contentType)
  }

  @Test
  fun `test SVG image request`() = imageTestApplication {
    val svgData = """<?xml version="1.0" encoding="UTF-8"?>
      <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
        <circle cx="50" cy="50" r="40" fill="red"/>
      </svg>""".toByteArray()

    val testFile = File(baseDir, "test.svg").apply {
      createNewFile()
      writeBytes(svgData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/svg+xml", contentType)

    // 验证响应内容
    val responseBytes = response.readBytes()
    assertTrue(svgData.contentEquals(responseBytes))
  }

  @Test
  fun `test ICO image request`() = imageTestApplication {
    val icoData = byteArrayOf(
      0x00, 0x00,
      0x01, 0x00,
      0x01, 0x00
    )

    val testFile = File(baseDir, "test.ico").apply {
      createNewFile()
      writeBytes(icoData)
    }

    val response = client.get("/image?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)

    // 验证Content-Type头
    val contentType = response.headers[HttpHeaders.ContentType]
    assertNotNull(contentType)
    assertEquals("image/x-icon", contentType)
  }
}