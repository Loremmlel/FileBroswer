package tenshi.hinanawi.filebrowser.platform

interface HevcSupportDetector {
  suspend fun isHevcSupported(): Boolean
}

expect fun createHevcSupportDetector(): HevcSupportDetector