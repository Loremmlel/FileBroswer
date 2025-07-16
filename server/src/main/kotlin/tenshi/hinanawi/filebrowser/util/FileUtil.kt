package tenshi.hinanawi.filebrowser.util

import tenshi.hinanawi.filebrowser.model.response.FileType
import java.io.File

internal val IMAGE_SUFFIX = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico")
internal val VIDEO_SUFFIX = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "ts", "m3u8")

internal val TEXT_SUFFIX = setOf(
  "txt",
  "md",
  "log",
  "json",
  "xml",
  "html",
  "css",
  "js",
  "java",
  "kt",
  "py",
  "c",
  "cpp",
  "h",
  "hpp",
  "cs",
  "go",
  "rb",
  "php",
  "sql",
  "ts",
  "tsx",
  "jsx",
  "vue",
  "sh",
  "bat",
  "ps1",
  "psm1"
)

internal fun File.getFileType(): FileType = when {
  isDirectory -> FileType.Folder
  // bugfix
  // 原来直接用的File.endsWith，发现如果参数是String会直接用File()构造，也就是说，endsWith实际上要传的是文件路径……
  // 比扩展名用这个方式更合适
  IMAGE_SUFFIX.contains(this.extension.lowercase()) -> FileType.Image
  VIDEO_SUFFIX.contains(this.extension.lowercase()) -> FileType.Video
  else -> FileType.Other
}

internal fun File.getContentType(): String = when (this.extension.lowercase()) {
  "jpg" -> "image/jpeg"
  "jpeg" -> "image/jpeg"
  "png" -> "image/png"
  "gif" -> "image/gif"
  "webp" -> "image/webp"
  "bmp" -> "image/bmp"
  "svg" -> "image/svg+xml"
  "ico" -> "image/x-icon"
  "mp4" -> "video/mp4"
  "mkv" -> "video/x-matroska"
  "avi" -> "video/x-msvideo"
  "mov" -> "video/quicktime"
  "wmv" -> "video/x-ms-wmv"
  "flv" -> "video/x-flv"
  "3gp" -> "video/3gpp"
  "ts" -> "video/mp2t"
  "m3u8" -> "application/vnd.apple.mpegurl"
  in TEXT_SUFFIX -> "text/plain"
  else -> "application/octet-stream"
}
