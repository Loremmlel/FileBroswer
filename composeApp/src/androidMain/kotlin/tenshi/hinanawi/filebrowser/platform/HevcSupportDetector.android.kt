package tenshi.hinanawi.filebrowser.platform

import android.media.MediaCodecList
import tenshi.hinanawi.filebrowser.util.printlnException

class AndroidHevcSupportDetector : HevcSupportDetector {
  override fun isHevcSupported(): Boolean = try {
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    val codecs = codecList.codecInfos

    for (codec in codecs) {
      if (codec.isEncoder) {
        continue
      }
      val supportedTypes = codec.supportedTypes
      for (type in supportedTypes) {
        if (type.lowercase() == "video/hevc" || type.lowercase() == "video/x-vnd.on2.vp9") {
          true
        }
      }
    }
    false
  } catch (e: Exception) {
    printlnException(e)
    false
  }
}

actual fun createHevcSupportDetector(): HevcSupportDetector = AndroidHevcSupportDetector()