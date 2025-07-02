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

  object Database {
    val driver: String get() = props.getProperty("DATABASE.DRIVER", "org.postgresql.Driver")
    val url: String get() = props.getProperty("DATABASE.URL", "jdbc:postgresql://localhost:5432/filebrowser")
    val user: String get() = props.getProperty("DATABASE.USER", "postgres")
    val password: String get() = props.getProperty("DATABASE.PASSWORD", "root")
    val maxPoolSize get() = props.getProperty("DATABASE.MAX_POOL_SIZE", "10").toInt()
    val minPoolSize get() = props.getProperty("DATABASE.MIN_POOL_SIZE", "2").toInt()
  }
}