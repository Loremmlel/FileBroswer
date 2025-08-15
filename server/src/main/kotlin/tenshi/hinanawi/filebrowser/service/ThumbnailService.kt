package tenshi.hinanawi.filebrowser.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.model.response.FileType
import tenshi.hinanawi.filebrowser.util.executeAndGetOutput
import tenshi.hinanawi.filebrowser.util.executeCommand
import tenshi.hinanawi.filebrowser.util.getFileType
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

  private val logger = LoggerFactory.getLogger(ThumbnailService::class.java)

  private val formatName by lazy {
    if (ImageIO.getWriterFormatNames().contains("webp")) "webp" else "png"
  }

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
    val originalImage = ImageIO.read(image) ?: run {
      // 如果ImageIO无法读取，可能是不支持的格式（如WebP）
      logger.warn("ImageIO无法读取图片格式，可能需要添加相应的ImageIO插件: ${image.absolutePath}")
      return null
    }

    try {
      val (scaledWidth, scaledHeight) = calculateScaledDimensions(
        originalImage.width, originalImage.height
      )

      // 使用更内存友好的缩放方式
      val outputImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
      val g2d = outputImage.createGraphics()

      try {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 直接绘制原图到目标尺寸，避免创建中间的scaledImage
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null)

        ByteArrayOutputStream().use { outputStream ->
          ImageIO.write(outputImage, formatName, outputStream)
          outputStream.toByteArray()
        }
      } finally {
        g2d.dispose()
      }
    } finally {
      // 显式释放原始图像资源
      originalImage.flush()
    }
  } catch (e: Exception) {
    logger.warn("创建缩略图失败: ${image.absolutePath}", e.message)
    null
  }

  private fun createVideoThumbnail(video: File): ByteArray? {
    val duration = getVideoDurationSeconds(video)
    if (duration <= 0.0) {
      logger.warn("不合法的视频时长: ${video.absolutePath}")
      return null
    }

    val startTime = duration * 0.1
    val endTime = duration * 0.9
    val randomSeekTime = Random.nextDouble(startTime, endTime)
    val seekPosition = formatDuration(randomSeekTime)

    val tempOutputFile = File.createTempFile("temp", ".png")
    try {
      val command = listOf(
        "ffmpeg",
        "-ss", seekPosition,
        "-i", video.absolutePath,
        "-vframes", "1",
        "-q:v", "2",
        "-y", tempOutputFile.absolutePath
      )
      val success = executeCommand(
        command = command,
        timeout = 15,
        unit = TimeUnit.SECONDS,
        logger = logger,
        processName = "ffmpeg创建视频缩略图"
      )
      if (!success) {
        logger.warn("ffmpeg创建视频缩略图失败: ${video.absolutePath}")
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
    // fix记录：process在windows和macos上表现不一样。windows你waitFor之后再读取输出，会失败。因为输出塞满缓冲区之后会阻塞线程
    val output = executeAndGetOutput(
      command = command,
      timeout = 10,
      unit = TimeUnit.SECONDS,
      logger = logger,
      processName = "ffprobe获取视频时长"
    ) ?: return -1.0
    parseDurationFromJson(output)
  } catch (e: Exception) {
    logger.warn("获取视频文件时长失败: ${video.absolutePath}", e.message)
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
    logger.warn("解析ffprobe json失败: $json", e.message)
    -1.0
  }

  private fun formatDuration(totalSeconds: Double): String {
    val hours = (totalSeconds / 3600).toInt()
    val minutes = (totalSeconds % 3600 / 60).toInt()
    val seconds = totalSeconds % 60
    return String.format(Locale.ENGLISH, "%02d:%02d:%06.3f", hours, minutes, seconds)
  }
}