package tenshi.hinanawi.filebrowser.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.util.getFileType
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.random.Random

class ThumbnailService {
  private val thumbnailMaxWidth: Int = 256
  private val thumbnailMaxHeight: Int = 256

  private val logger = LoggerFactory.getLogger("BusinessLogger")

  fun createThumbnail(file: File): ByteArray? {
    if (!file.exists() || !file.isFile) {
      return null
    }
    val fileType = file.getFileType()
    if (fileType != FileType.Image && fileType != FileType.Video) {
      return null
    }
    return when (fileType) {
      FileType.Image -> createImageThumbnail(file)
      FileType.Video -> createVideoThumbnail(file)
      else -> null
    }
  }

  private fun createImageThumbnail(image: File): ByteArray? = try {
    val originalImage = ImageIO.read(image)
    val (scaledWidth, scaledHeight) = calculateScaledDimensions(
      originalImage.width, originalImage.height
    )
    val scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
    val outputImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)

    val g2d = outputImage.createGraphics()

    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.drawImage(scaledImage, 0, 0, null)
    g2d.dispose()

    ByteArrayOutputStream().use { outputStream ->
      ImageIO.write(outputImage, "jpg", outputStream)
      outputStream.toByteArray()
    }
  } catch (e: Exception) {
    logger.warn("Failed to create image thumbnail for file: ${image.absolutePath}", e.message)
    null
  }

  private fun createVideoThumbnail(video: File): ByteArray? {
    val duration = getVideoDurationSeconds(video)
    if (duration <= 0.0) {
      logger.warn("Invalid video duration for file: ${video.absolutePath}")
      return null
    }

    val startTime = duration * 0.1
    val endTime = duration * 0.9
    val randomSeekTime = Random.nextDouble(startTime, endTime)
    val seekPosition = formatDuration(randomSeekTime)

    val tempOutputFile = File.createTempFile("temp", ".jpg")
    try {
      val command = listOf(
        "ffmpeg",
        "-ss", seekPosition,
        "-i", video.absolutePath,
        "-vframes", "1",
        "-q:v", "2",
        "-y", tempOutputFile.absolutePath
      )
      val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()
      val completed = process.waitFor(15, TimeUnit.SECONDS)
      if (!completed || process.exitValue() != 0) {
        logger.warn("ffmpeg failed to create video thumbnail for file: ${video.absolutePath}")
        val output = process.inputStream.bufferedReader().readText()
        logger.warn("ffmpeg output: $output")
        return null
      }
      return createImageThumbnail(tempOutputFile)
    } finally {
      if (tempOutputFile.exists()) {
        tempOutputFile.delete()
      }
    }
  }

  private fun calculateScaledDimensions(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
    if (originalWidth <= thumbnailMaxWidth && originalHeight <= thumbnailMaxHeight) {
      return originalWidth to originalHeight
    }
    val widthRatio = thumbnailMaxWidth.toDouble() / originalWidth
    val heightRatio = thumbnailMaxHeight.toDouble() / originalHeight

    val ratio = minOf(widthRatio, heightRatio)

    val scaledWidth = (originalWidth * ratio).toInt()
    val scaledHeight = (originalHeight * ratio).toInt()

    return scaledWidth to scaledHeight
  }

  private fun getVideoDurationSeconds(video: File): Double = try {
    val command = listOf(
      "ffprobe",
      "-v", "quiet",
      "-print_format", "json",
      "-show_format",
      "-show_streams",
      video.absolutePath
    )
    val process = ProcessBuilder(command)
      .redirectErrorStream(true)
      .start()
    val completed = process.waitFor(10, TimeUnit.SECONDS)
    val output = process.inputStream.bufferedReader().readText()

    if (!completed || process.exitValue() != 0) {
      logger.warn("ffprobe failed to get video duration for file: ${video.absolutePath}")
      logger.warn("ffprobe output: $output")
      -1.0
    }
    parseDurationFromJson(output)
  } catch (e: Exception) {
    logger.warn("Failed to get video duration for file: ${video.absolutePath}", e.message)
    -1.0
  }

  @Serializable
  private data class FFprobeOutput(
    val streams: List<FFStream>? = null,
    val format: FFFormat? = null
  )

  @Serializable
  private data class FFStream(
    @SerialName("codec_type")
    val codecType: String,
    val duration: String? = null
  )

  @Serializable
  private data class FFFormat(
    val duration: String? = null
  )

  private val jsonParser = Json { ignoreUnknownKeys = true }

  private fun parseDurationFromJson(json: String): Double = try {
    val output = jsonParser.decodeFromString<FFprobeOutput>(json)
    output.format?.duration?.toDoubleOrNull()
      ?: output.streams?.firstOrNull { it.codecType == "video" }?.duration?.toDoubleOrNull()
      ?: -1.0
  } catch (e: Exception) {
    logger.warn("Failed to parse video duration from json: $json", e.message)
    -1.0
  }

  private fun formatDuration(totalSeconds: Double): String {
    val hours = (totalSeconds / 3600).toInt()
    val minutes = (totalSeconds % 3600 / 60).toInt()
    val seconds = totalSeconds % 60
    return String.format(Locale.ENGLISH, "%02d:%02d:%06.3f", hours, minutes, seconds)
  }
}