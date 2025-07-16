package tenshi.hinanawi.filebrowser.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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

  private val logger = LoggerFactory.getLogger("BusinessLogger")

  suspend fun startTranscode(filePath: String): TranscodeStatus {
    val inputFile = File(AppConfig.basePath, filePath)
    val id = UUID.randomUUID().toString()
    val outputDir = File(cacheDir, id).apply { mkdirs() }
    val playlistFile = File(outputDir, "playlist.m3u8")

    val status = TranscodeStatus(
      id = id,
      status = TranscodeStatus.Enum.Pending,
      outputPath = "/video/$id/playlist.m3u8"
    )

    return try {
      val processBuilder = ProcessBuilder(
        "ffmpeg",
        "-i", inputFile.absolutePath,
        "-c:v", "libx264",
        "-crf", "25",
        "-preset", "fast",
        "-c:a", "copy",
        "-f", "hls",
        "-g", "90",
        "-hls_time", "9",
        "-hls_list_size", "0",
        "-hls_flags", "append_list+temp_file",
        "-hls_segment_filename", "${outputDir.absolutePath}/segment%04d.ts",
        playlistFile.absolutePath
      )
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
          val durationMatch = Regex("Duration: (\\d+):(\\d+):(\\d+\\.\\d+)").find(line)
          if (durationMatch != null) {
            val (h, m, s) = durationMatch.destructured
            duration = h.toDouble() * 3600 + m.toDouble() * 60 + s.toDouble()
          }

          val timeMatch = Regex("time=(\\d+):(\\d+):(\\d+\\.\\d+)").find(line)
          if (timeMatch != null && duration > 0) {
            val (h, m, s) = timeMatch.destructured
            val currentTime = h.toDouble() * 3600 + m.toDouble() * 60 + s.toDouble()
            val progress = (currentTime / duration).coerceAtMost(0.99)
            tasks[task.id]?.status?.let { status ->
              tasks[task.id] = task.copy(
                status = status.copy(progress = progress)
              )
            }
          }
        }
      }

      val exitCode = task.process.waitFor()
      logger.info("ffmpeg进程退出, 退出码: $exitCode")
      tasks[task.id]?.status?.let { status ->
        tasks[task.id] = task.copy(
          status = if (exitCode == 0) {
            status.copy(status = TranscodeStatus.Enum.Completed)
          } else {
            status.copy(status = TranscodeStatus.Enum.Error, error = "ffmpeg进程非正常退出, 退出码: $exitCode")
          }
        )
      }
    } catch (e: Exception) {
      tasks[task.id]?.status?.let { status ->
        tasks[task.id] = task.copy(
          status = status.copy(status = TranscodeStatus.Enum.Error, error = e.message)
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
    val task = tasks.remove(id) ?: return

    task.job?.cancel()
    task.process.destroyForcibly()

    try {
      task.outputDir.deleteRecursively()
    } catch (e: Exception) {
      logger.warn("删除视频转码缓存文件夹${task.outputDir.absolutePath}失败, $e, ${e.message}")
    }
  }

  fun cleanup() {
    tasks.keys.forEach { stopTranscode(it) }
    cacheDir.deleteRecursively()
  }
}