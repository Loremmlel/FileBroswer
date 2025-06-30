package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscodeStatus(
    val id: String,
    val status: TranscodeState,
    val createdAt: Long,
    val progress: Double = 0.0,
    val outputPath: String? = null,
    val error: String? = null
)

@Serializable
data class TranscodeRequest(
    val filePath: String,
    val quality: TranscodeQuality = TranscodeQuality.Medium
)

@Serializable
enum class TranscodeState {
    Pending,
    Processing,
    Completed,
    Error,
    Cancelled
}

@Serializable
enum class TranscodeQuality {
    High,
    Medium,
    Low;

    val crf get() = when(this) {
        High -> "23"
        Medium -> "25"
        Low -> "28"
    }
}