package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.TranscodeRepository
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus

sealed class TranscodeState {
  object Idle : TranscodeState()
  object Loading : TranscodeState()
  data class InProgress(val status: TranscodeStatus) : TranscodeState()
  data class Completed(val status: TranscodeStatus) : TranscodeState()
  data class Error(val message: String) : TranscodeState()
}

@Composable
fun rememberTranscodeState(
  videoPath: String,
  transcodeRepository: TranscodeRepository
): State<TranscodeState> {
  val state = remember(videoPath) { mutableStateOf<TranscodeState>(TranscodeState.Idle) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(videoPath) {
    if (state.value !is TranscodeState.Idle) {
      return@LaunchedEffect
    }
    try {
      state.value = TranscodeState.Loading
      val startStatus = transcodeRepository.startTranscode(videoPath)
      delay(100)
      transcodeRepository.observeTranscode(startStatus.id)
        .catch { e ->
          state.value = TranscodeState.Error("转码监控失败: ${e.message}")
        }
        .filterNotNull()
        .collect { status ->
          state.value = when {
            status.status == TranscodeStatus.Enum.Completed && status.progress >= 0.99 -> {
              TranscodeState.Completed(status)
            }
            else -> TranscodeState.InProgress(status)
          }
        }
    } catch (e: Exception) {
      state.value = TranscodeState.Error("转码启动失败: ${e.message}")
    }
  }

  DisposableEffect(videoPath) {
    onDispose {
      scope.launch {
        (state.value as? TranscodeState.InProgress)?.let {
          transcodeRepository.stopTranscode(it.status.id)
        }
      }
    }
  }

  return state
}