package tenshi.hinanawi.filebrowser.platform

import tenshi.hinanawi.filebrowser.util.printlnException

class DesktopHevcSupportDetector : HevcSupportDetector {
  override var detail: String = ""
  override var solution: List<String>? = null

  override fun isHevcSupported(): Boolean {
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
      if (version >= 10) {
        detail = "Windows $osVersion: 你的操作系统支持HEVC原生播放"
        true
      } else {
        detail = "Windows $osVersion: 你的操作系统不支持HEVC原生播放"
        solution = listOf(
          "检查你的硬件是否支持HEVC解码，例如Intel第七代及以上处理器或支持HEVC的独立显卡",
          "Windows 10及以上版本原生支持HEVC解码，建议升级操作系统",
          "备份重要文件",
          "确保设备已连接到电源",
          "下载Windows 10安装工具: https://www.microsoft.com/zh-cn/software-download/windows10",
          "运行安装工具并选择升级此电脑",
          "按照提示完成安装",
          "如果不想升级系统，可以安装第三方播放器如VLC或K-Lite Codec Pack"
        )
        false
      }
    } catch (e: Exception) {
      printlnException(e)
      detail = "Windows $osVersion: 检测HEVC支持时发生错误"
      solution = listOf(
        "报告异常: ",
        e.toString(),
        e.message ?: ""
      )
      false
    }
  }

  private fun checkMacHevcSupport(): Boolean {
    val osVersion = System.getProperty("os.version")
    return try {
      val parts = osVersion.split(".")
      val major = parts[0].toInt()
      val minor = parts.getOrNull(1)?.toInt() ?: 0

      if (major > 10 || (major == 10 && minor >= 13)) {
        detail = "MacOS $osVersion: 你的操作系统支持HEVC原生播放"
        true
      } else {
        detail = "MacOS $osVersion: 你的操作系统不支持HEVC原生播放"
        solution = listOf(
          "检查你的硬件是否支持HEVC解码，2015年后的Mac设备通常支持",
          "MacOS High Sierra (10.13)及以上版本原生支持HEVC解码",
          "升级你的操作系统到MacOS High Sierra 10.13或更高版本",
          "备份重要文件",
          "确保设备已连接到电源",
          "打开系统偏好设置，选择软件更新",
          "检查更新并安装最新的MacOS版本",
          "如果不能升级系统，可以安装第三方播放器如VLC或IINA"
        )
        false
      }
    } catch (e: Exception) {
      printlnException(e)
      detail = "MacOS $osVersion: 检测HEVC支持时发生错误"
      solution = listOf(
        "报告异常: ",
        e.toString(),
        e.message ?: ""
      )
      false
    }
  }

  private fun checkLinuxHevcSupport(): Boolean = try {
    val process = ProcessBuilder("ffmpeg", "-codecs").start()
    val output = process.inputStream.bufferedReader().readText()
    if (output.contains("hevc") || output.contains("h265")) {
      detail = "Linux & ffmpeg: 你的操作系统支持HEVC原生播放"
      true
    } else {
      detail = "Linux & ffmpeg: 你的操作系统不支持HEVC原生播放"
      solution = listOf(
        "检查你的硬件是否支持HEVC解码，例如支持HEVC的独立显卡",
        "Linux系统需要安装正确配置的ffmpeg或libav库以支持HEVC解码",
        "安装ffmpeg工具并确保其版本支持HEVC解码",
        "备份重要文件",
        "确保设备已连接到电源",
        "更新你的Linux发行版到最新版本",
        "使用包管理器安装最新版本的ffmpeg，例如: sudo apt update && sudo apt install ffmpeg",
        "对于Ubuntu/Debian系统，可能需要添加第三方PPA: sudo add-apt-repository ppa:mc3man/trusty-media",
        "如果仍然无法播放HEVC视频，请考虑安装第三方播放器，例如VLC或mpv"
      )
      false
    }
  } catch (e: Exception) {
    printlnException(e)
    detail = "Linux: 检测HEVC支持时发生错误，可能是ffmpeg未安装"
    solution = listOf(
      "请先安装ffmpeg: sudo apt install ffmpeg 或 sudo yum install ffmpeg",
      "报告异常: ",
      e.toString(),
      e.message ?: ""
    )
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = DesktopHevcSupportDetector()