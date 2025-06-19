package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
    val name: String,
    val size: String,
    val isDirectory: Boolean
)
