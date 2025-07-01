package tenshi.hinanawi.filebrowser.platform

import android.media.MediaCodecList
import tenshi.hinanawi.filebrowser.util.printlnException

class AndroidHevcSupportDetector : HevcSupportDetector {
  override var detail: String = ""
  override var solution: List<String>? = null

  override fun isHevcSupported(): Boolean = try {
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    val codecs = codecList.codecInfos
    var supported = false

    for (codec in codecs) {
      if (codec.isEncoder) {
        continue
      }
      val supportedTypes = codec.supportedTypes
      for (type in supportedTypes) {
        if (type.lowercase() == "video/hevc" || type.lowercase() == "video/x-vnd.on2.vp9") {
          detail = "Android设备: 检测到支持HEVC解码器 - ${codec.name}"
          supported = true
          break
        }
      }
      if (supported) break
    }

    if (!supported) {
      detail = "Android设备: 未检测到支持HEVC的解码器"
      solution = listOf(
        "你的设备可能不支持HEVC硬件解码",
        "检查你的设备是否运行Android 5.0 (Lollipop)或更高版本",
        "部分旧设备即使运行较新Android版本也可能不支持HEVC",
        "考虑使用第三方播放器如VLC，它可能提供软件解码支持",
        "如果是较旧设备，可能需要考虑升级到支持HEVC的新设备"
      )
    }

    supported
  } catch (e: Exception) {
    printlnException(e)
    detail = "Android设备: 检测HEVC支持时发生错误"
    solution = listOf(
      "报告异常: ",
      e.toString(),
      e.message ?: ""
    )
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = AndroidHevcSupportDetector()