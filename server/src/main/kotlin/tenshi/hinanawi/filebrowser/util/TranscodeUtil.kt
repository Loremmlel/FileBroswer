package tenshi.hinanawi.filebrowser.util

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.TranscodeQuality
import tenshi.hinanawi.filebrowser.model.TranscodeRequest
import tenshi.hinanawi.filebrowser.model.TranscodeState
import tenshi.hinanawi.filebrowser.model.TranscodeStatus
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class TranscodeManager {
    private val tasks = ConcurrentMap<String, TranscodeTask>()
    private val taskQueue = Channel<TranscodeTask>(Channel.UNLIMITED)
    private val _activeTasks = MutableStateFlow(0)
    val activeTasks: StateFlow<Int> = _activeTasks

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        File(AppConfig.cachePath).mkdirs()

        repeat(AppConfig.maxConcurrentTasks) {
            scope.launch {
                processTaskQueue()
            }
        }

        scope.launch {
            while (isActive) {
                delay(3.minutes)
                cleanupExpiredTasks()
            }
        }
    }

    suspend fun startTranscode(request: TranscodeRequest): TranscodeStatus {
        val id = UUID.randomUUID().toString()
        val outputDir = File(AppConfig.cachePath, id)
        outputDir.mkdir()

        val task = TranscodeTask(
            id = id,
            inputPath = request.filePath,
            outputDir = outputDir,
            quality = request.quality
        )
        tasks[id] = task
        taskQueue.send(task)

        return task.status.value
    }

    fun getStatus(id: String): TranscodeStatus? = tasks[id]?.status?.value

    fun stopTranscode(id: String): Boolean {
        val task = tasks[id] ?: return false
        task.cancel()
        tasks.remove(id)
        return true
    }

    private suspend fun processTaskQueue() {
        for (task in taskQueue) {
            if (task.isCancelled) {
                continue
            }
            _activeTasks.value++
            try {
                executeTranscode(task)
            } catch (e: Exception) {
                task.updateStatus {
                    it.copy(status = TranscodeState.Error, error = e.message)
                }
            } finally {
                _activeTasks.value--
            }
        }
    }

    private suspend fun executeTranscode(task: TranscodeTask) = withContext(Dispatchers.IO) {
        val playlistFile = File(task.outputDir, "playlist.m3u8")
        task.updateStatus {
            it.copy(status = TranscodeState.Processing, outputPath = playlistFile.absolutePath)
        }
        val process = ProcessBuilder().apply {
            command(buildFfmpegCommand(task, playlistFile))
            redirectErrorStream(false)
        }.start()
        task.process = process

        launch {
            monitorProgress(task, process)
        }

        val completed = withTimeoutOrNull(AppConfig.taskTimeoutMinutes.minutes) {
            process.waitFor() == 0
        }

        when {
            task.isCancelled -> {
                task.updateStatus { it.copy(status = TranscodeState.Cancelled) }
            }
            completed == true -> {
                task.updateStatus { it.copy(status = TranscodeState.Completed, progress = 1.0) }
            }
            completed == null -> {
                task.updateStatus { it.copy(status = TranscodeState.Error, error = "任务超时") }
            }
            else -> {
                task.updateStatus { it.copy(status = TranscodeState.Error, error = "转码失败") }
            }
        }
    }

    private suspend fun monitorProgress(task: TranscodeTask, process: Process) {
        // 简化的进度监控，实际进度需要解析ffmpeg输出
        var progress = 0.0
        while (process.isAlive && !task.isCancelled) {
            delay(1000)
            progress = minOf(progress + 0.01, 0.99)
            task.updateStatus {
                it.copy(progress = progress)
            }
        }
    }

    private fun cleanupExpiredTasks() {
        val now = System.currentTimeMillis()
        val expiredTasks = tasks.values.filter {
            val age = now - it.status.value.createdAt
            age > AppConfig.taskTimeoutMinutes * 60 * 1000
                    && it.status.value.status in listOf(
                TranscodeState.Completed, TranscodeState.Error, TranscodeState.Cancelled
            )
        }
        expiredTasks.forEach {
            it.cleanup()
            tasks.remove(it.id)
        }
    }

    private fun buildFfmpegCommand(task: TranscodeTask, playlistFile: File): List<String> {
        val segmentPath = File(task.outputDir, "segment%04d.ts").absolutePath

        return listOf(
            "ffmpeg",
            "-hwaccel", "auto",
            "-i", task.inputPath,
            "-c:v", "libx264",
            "-preset", "medium",
            "-crf", task.quality.crf,
            "-c:a", "aac",
            "-f", "hls",
            "hls_time", "10",
            "-hls_list_size", "0",
            "-hls_segment_filename", segmentPath,
            playlistFile.absolutePath
        )
    }

    fun shutdown() {
        scope.cancel()
        tasks.values.forEach { it.cleanup() }
        tasks.clear()
    }
}

private class TranscodeTask(
    val id: String,
    val inputPath: String,
    val outputDir: File,
    val quality: TranscodeQuality
) {
    private val _status = MutableStateFlow(
        TranscodeStatus(
            id = id,
            status = TranscodeState.Pending,
            createdAt = System.currentTimeMillis()
        )
    )
    val status: StateFlow<TranscodeStatus> = _status

    var process: Process? = null
    var isCancelled = false
        private set

    fun updateStatus(update: (TranscodeStatus) -> TranscodeStatus) {
        _status.value = update(_status.value)
    }

    fun cancel() {
        isCancelled = true
        process?.destroyForcibly()
        cleanup()
    }

    fun cleanup() {
        try {
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
        } catch (e: Exception) {
            println("清理临时文件失败: ${e.message}")
        }
    }
}