package tenshi.hinanawi.filebrowser.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

class TranscodeService {
  data class TranscodeTask(
    val id: String,
    val process: Process,
    val status: TranscodeStatus,
    val outputDir: File,
    var job: Job? = null
  )

  private val tasks = ConcurrentHashMap<String, TranscodeTask>()
  private val cacheDir = File(AppConfig.cachePath).apply { mkdirs() }
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val logger = LoggerFactory.getLogger(TranscodeService::class.java)

  private val durationPattern by lazy { Pattern.compile("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)") }
  private val timePattern by lazy { Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)") }

  private val hwaccelConfig by lazy {
    runBlocking {
      detectHardwareAcceleration()
    }
  }

  suspend fun startTranscode(video: File): TranscodeStatus {
    val id = UUID.randomUUID().toString()
    val outputDir = File(cacheDir, id).apply { mkdirs() }
    val playlistFile = File(outputDir, "playlist.m3u8")

    val status = TranscodeStatus(
      id = id,
      status = TranscodeStatus.Enum.Pending,
      outputPath = "/video/$id/playlist.m3u8"
    )

    return try {
      logger.info("使用编码器: ${hwaccelConfig.encoder}, 预设: ${hwaccelConfig.preset}")
      val command = buildList {
        add("ffmpeg")

        hwaccelConfig.hwaccel?.let {
          add("-hwaccel")
          add(it)
        }
        hwaccelConfig.hwaccelOutputFormat?.let {
          add("-hwaccel_output_format")
          add(it)
        }

        add("-i")
        add(video.absolutePath)

        add("-c:v")
        add(hwaccelConfig.encoder)
        add("-preset")
        add(hwaccelConfig.preset)

        addAll(hwaccelConfig.extraArgs)

        add("-c:a")
        add("copy")

        add("-f")
        add("hls")
        add("-g")
        add("90")
        add("-hls_time")
        add("9")
        add("-hls_list_size")
        add("0")
        add("-hls_flags")
        add("append_list+temp_file")
        add("-hls_segment_filename")
        add("${outputDir.absolutePath}/segment%04d.ts")

        add(playlistFile.absolutePath)
      }
      logger.info("转码命令: ${command.joinToString(" ")}")

      val processBuilder = ProcessBuilder(command)
      val process = processBuilder.start()
      val updatedStatus = status.copy(status = TranscodeStatus.Enum.Processing)

      val task = TranscodeTask(id, process, updatedStatus, outputDir)
      tasks[id] = task

      task.job = scope.launch {
        monitorTranscode(task)
        delay(30.minutes)
        stopTranscode(id)
      }

      waitForFirstSegment(outputDir)

      tasks[id]?.status?.copy(status = TranscodeStatus.Enum.Completed) ?: updatedStatus
    } catch (e: Exception) {
      logger.warn("startTranscode方法失败, $e, ${e.message}")
      status.copy(status = TranscodeStatus.Enum.Error, error = e.message)
    }
  }

  private suspend fun monitorTranscode(task: TranscodeTask) = withContext(Dispatchers.IO) {
    val errorReader = task.process.errorStream.bufferedReader()
    var duration = 0.0

    try {
      errorReader.useLines { lines ->
        for (line in lines) {
          val durationMatch = durationPattern.toRegex().find(line)
          if (durationMatch != null) {
            val (h, m, s) = durationMatch.destructured
            duration = h.toDouble() * 3600 + m.toDouble() * 60 + s.toDouble()
          }

          val timeMatch = timePattern.toRegex().find(line)
          if (timeMatch != null && duration > 0) {
            val (h, m, s) = timeMatch.destructured
            val currentTime = h.toDouble() * 3600 + m.toDouble() * 60 + s.toDouble()
            val progress = (currentTime / duration).coerceAtMost(0.99)
            tasks.computeIfPresent(task.id) { _, currentTask ->
              currentTask.copy(
                status = currentTask.status.copy(progress = progress)
              )
            }
          }
        }
      }

      val exitCode = task.process.waitFor()
      logger.info("ffmpeg进程退出, 退出码: $exitCode")
      tasks.computeIfPresent(task.id) { _, currentTask ->
        currentTask.copy(
          status = if (exitCode == 0) {
            currentTask.status.copy(status = TranscodeStatus.Enum.Completed)
          } else {
            currentTask.status.copy(status = TranscodeStatus.Enum.Error, error = "ffmpeg进程非正常退出, 退出码: $exitCode")
          }
        )
      }
    } catch (e: Exception) {
      tasks.computeIfPresent(task.id) { _, currentTask ->
        currentTask.copy(
          status = currentTask.status.copy(status = TranscodeStatus.Enum.Error, error = e.message)
        )
      }
    }
  }

  private suspend fun waitForFirstSegment(outputDir: File) = withContext(Dispatchers.IO) {
    val playlistFile = File(outputDir, "playlist.m3u8")
    val firstSegment = File(outputDir, "segment0000.ts")

    repeat(300) {
      if (playlistFile.exists() && firstSegment.exists()) {
        return@withContext
      }
      delay(100)
    }
  }

  fun getStatus(id: String): TranscodeStatus? = tasks[id]?.status

  fun stopTranscode(id: String) {
    val task = tasks.remove(id) ?: run {
      logger.warn("试图停止不存在的转码任务, id: $id")
      return
    }

    task.job?.cancel()
    task.process.destroyForcibly()

    try {
      task.outputDir.deleteRecursively()
    } catch (e: Exception) {
      logger.warn("删除视频转码缓存文件夹${task.outputDir.absolutePath}失败, $e, ${e.message}")
    }
  }

  fun cleanup() {
    try {
      tasks.keys.forEach { stopTranscode(it) }
      cacheDir.deleteRecursively()
      logger.info("转码缓存清理成功")
    }catch (e: Exception) {
      logger.warn("转码缓存清理失败", e)
    }
  }

  private data class HwaccelConfig(
    val encoder: String,
    val preset: String,
    val hwaccel: String? = null,
    val hwaccelOutputFormat: String? = null,
    val extraArgs: List<String> = emptyList()
  )

  private suspend fun detectHardwareAcceleration(): HwaccelConfig = withContext(Dispatchers.IO) {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    logger.info("当前系统: $osName, 架构: $osArch")

    when {
      // macOS
      osName.contains("mac") -> {
        if (checkVideoToolboxSupport()) {
          logger.info("使用macOS VideoToolbox硬件加速")
          HwaccelConfig(
            encoder = "h264_videotoolbox",
            preset = "medium",
            extraArgs = listOf("-allow_sw", "1", "-realtime", "0")
          )
        } else {
          logger.info("Video Toolbox不可用， 使用CPU编码")
          getCpuConfig()
        }
      }
      // Windows
      osName.contains("windows") -> {
        when {
          checkNvidiaNvencSupport() -> {
            logger.info("使用NVIDIA NVENC硬件加速")
            HwaccelConfig(
              encoder = "h264_nvenc",
              preset = "p6",
              hwaccel = "cuda",
              hwaccelOutputFormat = "cuda"
            )
          }

          checkIntelQsvSupport() -> {
            logger.info("使用Intel Quick Sync Video硬件加速")
            HwaccelConfig(
              encoder = "h264_qsv",
              preset = "medium",
              hwaccel = "qsv",
              hwaccelOutputFormat = "qsv"
            )
          }

          checkAmdAmfSupport() -> {
            logger.info("使用AMD AMF硬件加速")
            HwaccelConfig(
              encoder = "h264_amf",
              preset = "speed",
              extraArgs = listOf("-usage", "transcoding", "-profile", "main")
            )
          }

          else -> {
            logger.info("硬件加速不可用，使用CPU编码")
            getCpuConfig()
          }
        }
      }

      else -> {
        logger.info("未知操作系统或者Linux，使用CPU编码")
        getCpuConfig()
      }
    }
  }

  private fun getCpuConfig(): HwaccelConfig = HwaccelConfig(
    encoder = "libx264",
    preset = "fast",
    extraArgs = listOf("-crf", "25")
  )

  private suspend fun checkVideoToolboxSupport(): Boolean = withContext(Dispatchers.IO) {
    try {
      val process = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders").start()
      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()
      output.contains("h264_videotoolbox")
    } catch (e: Exception) {
      logger.warn("检测Video Toolbox硬件加速失败, $e, ${e.message}")
      false
    }
  }

  private suspend fun checkNvidiaNvencSupport(): Boolean = withContext(Dispatchers.IO) {
    try {
      val nvidiaProcess = ProcessBuilder("nvidia-smi").start()
      val nvidiaExitCode = nvidiaProcess.waitFor()

      if (nvidiaExitCode != 0) {
        return@withContext false
      }

      val ffmpegProcess = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders").start()
      val output = ffmpegProcess.inputStream.bufferedReader().readText()
      ffmpegProcess.waitFor()

      output.contains("h264_nvenc")
    } catch (e: Exception) {
      logger.warn("检测NVIDIA NVENC硬件加速失败, $e, ${e.message}")
      false
    }
  }

  private suspend fun checkIntelQsvSupport(): Boolean = withContext(Dispatchers.IO) {
    try {
      val process = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders").start()
      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()
      output.contains("h264_qsv")
    } catch (e: Exception) {
      logger.warn("检测 Intel QSV 支持时出错: ${e.message}")
      false
    }
  }

  private suspend fun checkAmdAmfSupport(): Boolean = withContext(Dispatchers.IO) {
    try {
      val process = ProcessBuilder("ffmpeg", "-hide_banner", "-encoders").start()
      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()
      output.contains("h264_amf")
    } catch (e: Exception) {
      logger.warn("检测 AMD AMF 支持时出错: ${e.message}")
      false
    }
  }
}