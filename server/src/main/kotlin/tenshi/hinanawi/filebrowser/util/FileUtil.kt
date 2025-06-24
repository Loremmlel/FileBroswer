package tenshi.hinanawi.filebrowser.util

import tenshi.hinanawi.filebrowser.model.FileType
import java.io.File

internal val IMAGE_SUFFIX = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg", ".ico")
internal val VIDEO_SUFFIX = listOf(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".3gp")

internal fun File.getFileType(): FileType = when {
    isDirectory -> FileType.Folder
    IMAGE_SUFFIX.any { this.endsWith(it) } -> FileType.Image
    VIDEO_SUFFIX.any { this.endsWith(it) } -> FileType.Video
    else -> FileType.Other
}