package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val code: Int,
    val message: String?,
    val data: T?
)