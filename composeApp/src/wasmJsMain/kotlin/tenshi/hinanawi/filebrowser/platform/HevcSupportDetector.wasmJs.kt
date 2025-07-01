package tenshi.hinanawi.filebrowser.platform

import kotlinx.browser.window
import org.w3c.dom.CanPlayTypeResult
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.MAYBE
import org.w3c.dom.PROBABLY
import org.w3c.dom.mediasource.MediaSource
import tenshi.hinanawi.filebrowser.util.printlnException

class WasmJsHevcSupportDetector : HevcSupportDetector {

  override var detail: String = ""
  override var solution: List<String>? = null
  private val hevcTypes = listOf(
    "video/mp4; codecs=\"hev1.1.6.L93.B0\"",
    "video/mp4; codecs=\"hvc1.1.6.L93.B0\"",
    "video/mp4; codecs=\"hev1\"",
    "video/mp4; codecs=\"hvc1\""
  )

  override fun isHevcSupported(): Boolean = try {
    val browserInfo = detectBrowser()
    detail = "浏览器信息: $browserInfo"

    val isVideoElementSupported = checkVideoElementSupport()
    val isMediaSourceSupported = checkMediaSourceSupport()

    if (isVideoElementSupported || isMediaSourceSupported) {
      detail = "浏览器信息: $browserInfo\nHEVC原生播放支持: 是"
      true
    } else {
      detail = "浏览器信息: $browserInfo\nHEVC原生播放支持: 否"

      // 根据浏览器类型提供更具针对性的解决方案
      solution = when {
        browserInfo.contains("Chrome") -> listOf(
          "Chrome浏览器需要版本105或更高才能支持HEVC",
          "请升级到最新版Chrome",
          "如果已是最新版本，请确认系统是否支持HEVC硬件解码"
        )

        browserInfo.contains("Firefox") -> listOf(
          "Firefox目前不支持HEVC原生播放",
          "建议使用Edge、Chrome或Safari等支持HEVC的浏览器",
          "或者下载视频后使用本地播放器播放"
        )

        browserInfo.contains("Safari") -> listOf(
          "Safari 11及以上版本支持HEVC",
          "请升级到最新版Safari",
          "确保你的macOS版本为High Sierra (10.13)或更高"
        )

        browserInfo.contains("Edge") -> listOf(
          "Edge基于Chromium的版本(79+)支持HEVC",
          "请升级到最新版Edge",
          "如果已是最新版本，请确认系统是否支持HEVC硬件解码"
        )

        else -> listOf(
          "你的浏览器可能不支持HEVC原生播放",
          "建议使用最新版本的Edge、Chrome或Safari",
          "或者下载视频后使用本地播放器播放"
        )
      }
      false
    }
  } catch (e: Exception) {
    printlnException(e)
    detail = "浏览器HEVC支持检测失败"
    solution = listOf(
      "报告异常: ",
      e.toString(),
      e.message ?: ""
    )
    false
  }

  private fun detectBrowser(): String {
    val userAgent = window.navigator.userAgent

    // 使用简化的浏览器检测方法，避免使用js()函数
    return when {
      userAgent.contains("Chrome") && !userAgent.contains("Edg/") && !userAgent.contains("OPR/") ->
        "Chrome ${extractVersion(userAgent, "Chrome/")}"

      userAgent.contains("Firefox") ->
        "Firefox ${extractVersion(userAgent, "Firefox/")}"

      userAgent.contains("Safari") && !userAgent.contains("Chrome") && !userAgent.contains("Edg/") ->
        "Safari ${extractVersion(userAgent, "Version/")}"

      userAgent.contains("Edg/") ->
        "Edge ${extractVersion(userAgent, "Edg/")}"

      userAgent.contains("OPR/") || userAgent.contains("Opera") ->
        "Opera ${extractVersion(userAgent, if (userAgent.contains("OPR/")) "OPR/" else "Opera/")}"

      userAgent.contains("Trident") || userAgent.contains("MSIE") ->
        "Internet Explorer"

      else -> "未知浏览器 ($userAgent)"
    }
  }

  private fun extractVersion(userAgent: String, versionMarker: String): String = try {
    val startPos = userAgent.indexOf(versionMarker)
    if (startPos == -1) "未知版本"

    val startOfVersion = startPos + versionMarker.length
    val endOfVersion = userAgent.indexOf(" ", startOfVersion)

    if (endOfVersion == -1)
      userAgent.substring(startOfVersion)
    else
      userAgent.substring(startOfVersion, endOfVersion)
  } catch (_: Exception) {
    "未知版本"
  }


  private fun checkVideoElementSupport(): Boolean {
    val video = window.document.createElement("video") as HTMLVideoElement

    return hevcTypes.any {
      val canPlayTypeResult = video.canPlayType(it)
      canPlayTypeResult == CanPlayTypeResult.PROBABLY || canPlayTypeResult == CanPlayTypeResult.MAYBE
    }
  }

  private fun checkMediaSourceSupport(): Boolean = try {
    hevcTypes.any {
      MediaSource.isTypeSupported(it)
    }
  } catch (e: Exception) {
    printlnException(e)
    solution = listOf(
      "报告异常: ",
      e.toString(),
      e.message ?: ""
    )
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = WasmJsHevcSupportDetector()