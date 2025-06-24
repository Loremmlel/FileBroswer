package tenshi.hinanawi.filebrowser

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertNotNull
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.plugins.files
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.test.*

class FilesEndpointTest {
    private lateinit var baseDir: File

    @BeforeTest
    fun setUp() {
        baseDir = Files.createTempDirectory("testBaseDir").toFile()

        val propsField = AppConfig::class.java.getDeclaredField("props")
        propsField.isAccessible = true

        val props = propsField.get(AppConfig) as Properties
        props.setProperty("BASE_DIR", baseDir.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        baseDir.deleteRecursively()
    }

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
        val client = createClient {
            followRedirects = false
        }
        val response = client.get("/files")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        val parsed = Json.decodeFromString<Response<FileInfo>>(body)
        assertEquals(400, parsed.code)
        assertEquals(Message.FilesNotFound, parsed.message)
    }

    @Test
    fun `test invalid path`() = fileTestApplication {
        val response = client.get("/files?path=invalid")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        val parsed = Json.decodeFromString<Response<FileInfo>>(body)
        assertEquals(400, parsed.code)
        assertEquals(Message.FilesNotFound, parsed.message)
    }

    @Test
    fun `test path traversal attempt`() = fileTestApplication {
        val response = client.get("/files?path=/../../")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        val body = response.bodyAsText()
        val parsed = Json.decodeFromString<Response<FileInfo>>(body)
        assertEquals(403, parsed.code)
        assertEquals(Message.FilesForbidden, parsed.message)
    }

    @Test
    fun `test non directory path`() = fileTestApplication {
        val file = File(baseDir, "test file.txt").apply {
            createNewFile()
        }
        val response = client.get("/files?path=/${file.name}")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        val parsed = Json.decodeFromString<Response<FileInfo>>(body)
        assertEquals(400, parsed.code)
        assertEquals(Message.FilesIsNotDirectory, parsed.message)
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
        assertTrue {
            items.any {
                it.name == file1.name
                        && it.path == "/testDir/${file1.name}"
                        && it.type == FileType.Other
            }
        }
        assertTrue {
            items.any {
                it.name == file2.name
                        && it.path == "/testDir/${file2.name}"
                        && it.type == FileType.Other
            }
        }
        assertTrue {
            items.any {
                it.name == subDir.name
                        && it.path == "/testDir/${subDir.name}"
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
        val parsed = Json.decodeFromString<Response<FileInfo>>(body)
        assertEquals(500, parsed.code)
        assertEquals(Message.InternalServerError, parsed.message)
        assertNull(parsed.data)
    }
}