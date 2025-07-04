package tenshi.hinanawi.filebrowser.route

import tenshi.hinanawi.filebrowser.config.AppConfig
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class BaseEndpointTest {
  protected lateinit var baseDir: File

  @BeforeTest
  fun setUp() {
    baseDir = Files.createTempDirectory("testRandomBaseDir").toFile()

    val propsField = AppConfig::class.java.getDeclaredField("props")
    propsField.isAccessible = true

    val props = propsField.get(AppConfig) as Properties
    props.setProperty("BASE_DIR", baseDir.absolutePath)
  }

  @AfterTest
  fun tearDown() {
    baseDir.deleteRecursively()
  }

  protected val isWindows: Boolean get() = System.getProperty("os.name").lowercase().contains("win")

  protected fun String.normalizedPath(): String {
    if (isWindows) {
      return replace("/", "\\")
    }
    return this
  }
}