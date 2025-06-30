package tenshi.hinanawi.filebrowser.config

import java.util.*

internal object AppConfig {
  private val props = Properties()

  init {
    val inputStream = AppConfig::class.java.classLoader.getResourceAsStream(".env")
    inputStream?.let {
      props.load(it)
    } ?: throw IllegalStateException("加载env文件失败")
  }

  const val CACHE_DIR_NAME = ".cache"

  val basePath: String get() = props.getProperty("BASE_DIR", "\$HOME/Movies")
  val cachePath: String get() = "${basePath}/$CACHE_DIR_NAME"

  val maxConcurrentTasks get() = props.getProperty("MAX_CONCURRENT_TASKS", "3").toInt()
  val taskTimeoutMinutes get() = props.getProperty("TASK_TIMEOUT_MINUTES", "30").toLong()
}