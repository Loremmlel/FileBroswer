package tenshi.hinanawi.filebrowser.plugin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.Json
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.route.BaseEndpointTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PathValidatorTest : BaseEndpointTest() {

  private fun pathValidatorTestApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult =
    testApplication {
      install(ContentNegotiation) {
        json()
      }
      application {
        routing {
          // 创建一个简单的测试路由来测试PathValidator插件
          route("/test") {
            install(PathValidator)
            get {
              call.respond(HttpStatusCode.OK, Response(200, Message.Success, "OK"))
            }
          }
        }
      }
      block()
    }

  @Test
  fun `test missing path parameter`() = pathValidatorTestApplication {
    val response = client.get("/test")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test invalid path format - not starting with slash`() = pathValidatorTestApplication {
    val response = client.get("/test?path=invalid")
    assertEquals(HttpStatusCode.BadRequest, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(400, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test path traversal attempt`() = pathValidatorTestApplication {
    val response = client.get("/test?path=/../../")
    assertEquals(HttpStatusCode.Forbidden, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(403, parsed.code)
    assertEquals(Message.FilesForbidden, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test path traversal with different patterns`() = pathValidatorTestApplication {
    val testCases = listOf(
      "/../../etc",
      "/../../../root",
      "/folder/../../../secrets",
      "/./../../hidden"
    )

    for (pathCase in testCases) {
      val response = client.get("/test?path=$pathCase")
      assertEquals(HttpStatusCode.Forbidden, response.status, "Failed for path: $pathCase")
      val parsed = Json.decodeFromString<Response<Unit>>(response.bodyAsText())
      assertEquals(403, parsed.code)
      assertEquals(Message.FilesForbidden, parsed.message)
    }
  }

  @Test
  fun `test non-existent file path`() = pathValidatorTestApplication {
    val response = client.get("/test?path=/non-existent-file.txt")
    assertEquals(HttpStatusCode.NotFound, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<Unit>>(body)
    assertEquals(404, parsed.code)
    assertEquals(Message.FilesNotFound, parsed.message)
    assertNull(parsed.data)
  }

  @Test
  fun `test valid existing file path`() = pathValidatorTestApplication {
    val testFile = File(baseDir, "valid-file.txt").apply { createNewFile() }

    val response = client.get("/test?path=/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<String>>(body)
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
    assertEquals("OK", parsed.data)
  }

  @Test
  fun `test valid existing directory path`() = pathValidatorTestApplication {
    val testDir = File(baseDir, "valid-dir").apply { mkdir() }

    val response = client.get("/test?path=/${testDir.name}")
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.bodyAsText()
    val parsed = Json.decodeFromString<Response<String>>(body)
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
    assertEquals("OK", parsed.data)
  }

  @Test
  fun `test exception handling with invalid characters`() = pathValidatorTestApplication {
    val invalidPath = "/\u0000"

    val response = client.get("/test") {
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
  fun `test nested directory path validation`() = pathValidatorTestApplication {
    val parentDir = File(baseDir, "parent").apply { mkdir() }
    val childDir = File(parentDir, "child").apply { mkdir() }
    val testFile = File(childDir, "nested-file.txt").apply { createNewFile() }

    val response = client.get("/test?path=/parent/child/${testFile.name}")
    assertEquals(HttpStatusCode.OK, response.status)
    val parsed = Json.decodeFromString<Response<String>>(response.bodyAsText())
    assertEquals(200, parsed.code)
    assertEquals(Message.Success, parsed.message)
  }
}