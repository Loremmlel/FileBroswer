package tenshi.hinanawi.filebrowser.platform

interface HevcSupportDetector {
  fun isHevcSupported(): Boolean
}

expect fun createHevcSupportDetector(): HevcSupportDetector