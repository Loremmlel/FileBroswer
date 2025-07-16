package tenshi.hinanawi.filebrowser.model.response

import kotlinx.serialization.Serializable

@Serializable
data class TranscodeStatus(
  val id: String,
  val status: Enum,
  val progress: Double = 0.0,
  val outputPath: String? = null,
  val error: String? = null
) {
  @Serializable
  enum class Enum {
    Pending,
    Processing,
    Completed,
    Error
  }
}