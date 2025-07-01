package tenshi.hinanawi.filebrowser.platform

interface HevcSupportDetector {
  var detail: String
  var solution: List<String>?
  fun isHevcSupported(): Boolean
}

expect fun createHevcSupportDetector(): HevcSupportDetector