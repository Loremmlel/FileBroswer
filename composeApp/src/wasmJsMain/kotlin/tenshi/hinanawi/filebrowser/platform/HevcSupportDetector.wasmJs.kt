package tenshi.hinanawi.filebrowser.platform

import kotlinx.browser.window
import org.w3c.dom.CanPlayTypeResult
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.MAYBE
import org.w3c.dom.PROBABLY
import org.w3c.dom.mediasource.MediaSource
import tenshi.hinanawi.filebrowser.util.printlnException

class WasmJsHevcSupportDetector : HevcSupportDetector {
  private val hevcTypes = listOf(
    "video/mp4; codecs=\"hev1.1.6.L93.B0\"",
    "video/mp4; codecs=\"hvc1.1.6.L93.B0\"",
    "video/mp4; codecs=\"hev1\"",
    "video/mp4; codecs=\"hvc1\""
  )

  override fun isHevcSupported(): Boolean = try {
    checkVideoElementSupport() || checkMediaSourceSupport()
  } catch (e : Exception) {
    printlnException(e)
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
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = WasmJsHevcSupportDetector()