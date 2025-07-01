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
    val userAgent = window.navigator.userAgent
    detail = "浏览器信息: $userAgent"

    val isVideoElementSupported = checkVideoElementSupport()
    val isMediaSourceSupported = checkMediaSourceSupport()

    if (isVideoElementSupported || isMediaSourceSupported) {
        detail = "浏览器信息: $userAgent\nHEVC原生播放支持: 是"
        true
    } else {
        detail = "浏览器信息: $userAgent\nHEVC原生播放支持: 否"
        solution = listOf(
            "如果使用的是Chrome，请升级到版本105或更高。",
            "如果使用的是Firefox，目前不支持HEVC，请考虑使用其他浏览器。",
            "如果使用的是Safari，请确保版本支持HEVC。",
            "如果仍有问题，请检查系统是否支持HEVC解码。",
            "考虑使用支持HEVC的浏览器，如最新版本的Edge或Safari"
        )
        false
    }
  } catch (e : Exception) {
    printlnException(e)
    detail = "浏览器HEVC支持检测失败"
    solution = listOf(
      "报告异常: ",
      e.toString(),
      e.message ?: ""
    )
    false
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