package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
enum class FileType {
    Folder,
    Image,
    Video,
    Other;

    override fun toString(): String = this.name
}

fun String.parseFileType(): FileType = when (this) {
    FileType.Folder.name -> FileType.Folder
    FileType.Image.name -> FileType.Image
    FileType.Video.name -> FileType.Video
    FileType.Other.name -> FileType.Other
    else -> throw IllegalArgumentException("${this}不能被转化为FileType")
}