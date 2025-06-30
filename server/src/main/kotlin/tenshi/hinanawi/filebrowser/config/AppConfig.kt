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

  val BASE_DIR: String get() = props.getProperty("BASE_DIR", "\$HOME/Movies")
  val CACHE_DIR: String get() = "${BASE_DIR}/$CACHE_DIR_NAME"
}