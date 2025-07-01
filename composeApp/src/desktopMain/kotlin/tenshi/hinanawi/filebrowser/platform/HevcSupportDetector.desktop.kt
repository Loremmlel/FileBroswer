package tenshi.hinanawi.filebrowser.platform

class DesktopHevcSupportDetector : HevcSupportDetector {
  override suspend fun isHevcSupported(): Boolean {
    val osName = System.getProperty("os.name").lowercase()
    return when {
      osName.contains("windows") -> checkWindowsHevcSupport()
      osName.contains("mac") -> checkMacHevcSupport()
      osName.contains("linux") -> checkLinuxHevcSupport()
      else -> false
    }
  }

  private fun checkWindowsHevcSupport(): Boolean {
    val osVersion = System.getProperty("os.version")
    return try {
      val version = osVersion.split(".")[0].toInt()
      version >= 10
    } catch (e: Exception) {
      println("$e; ${e.message}")
      false
    }
  }

  private fun checkMacHevcSupport(): Boolean {
    val osVersion = System.getProperty("os.version")
    return try {
      val parts = osVersion.split(".")
      val major = parts[0].toInt()
      val minor = parts.getOrNull(1)?.toInt() ?: 0

      major > 10 || (major == 10 && minor >= 13)
    } catch (e: Exception) {
      false
    }
  }

  private fun checkLinuxHevcSupport(): Boolean = try {
    val process = ProcessBuilder("ffmpeg", "-codecs").start()
    val output = process.inputStream.bufferedReader().readText()
    output.contains("hevc") || output.contains("h265")
  } catch (e: Exception) {
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = DesktopHevcSupportDetector()