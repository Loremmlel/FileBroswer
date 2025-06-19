package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class FileResponse(
    val name: String,
    val size: String,
    val isDirectory: Boolean
)
