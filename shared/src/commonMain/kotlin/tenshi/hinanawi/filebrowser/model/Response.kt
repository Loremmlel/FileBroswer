package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class ResponseWithoutData(
  val code: Int,
  val message: Message
)

@Serializable
data class Response<T>(
  val code: Int,
  val message: Message,
  val data: T?
)