package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.OnlineTranscodeRepository
import tenshi.hinanawi.filebrowser.data.repo.TranscodeRepository
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus

sealed class TranscodeUiState {
  object Idle : TranscodeUiState()
  object Loading : TranscodeUiState()
  data class InProgress(val status: TranscodeStatus) : TranscodeUiState()
  data class Completed(val status: TranscodeStatus) : TranscodeUiState()
  data class Error(val message: String) : TranscodeUiState()
}

@Composable
fun rememberTranscodeState(
  videoPath: String,
  transcodeRepository: TranscodeRepository
): State<TranscodeUiState> {
  val state = remember(videoPath) { mutableStateOf<TranscodeUiState>(TranscodeUiState.Idle) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(videoPath) {
    if (state.value !is TranscodeUiState.Idle) {
      return@LaunchedEffect
    }
    try {
      state.value = TranscodeUiState.Loading
      val startStatus = transcodeRepository.startTranscode(videoPath)
      delay(100)
      transcodeRepository.observeTranscode(startStatus.id)
        .catch { e ->
          state.value = TranscodeUiState.Error("转码监控失败: ${e.message}")
        }
        .filterNotNull()
        .collect { status ->
          state.value = when {
            status.status == TranscodeStatus.Enum.Completed && status.progress >= 0.99 -> {
              TranscodeUiState.Completed(status)
            }

            else -> TranscodeUiState.InProgress(status)
          }
        }
    } catch (e: Exception) {
      state.value = TranscodeUiState.Error("转码启动失败: ${e.message}")
    }
  }

  DisposableEffect(videoPath) {
    onDispose {
      scope.launch {
        (state.value as? TranscodeUiState.InProgress)?.let {
          transcodeRepository.stopTranscode(it.status.id)
        }
      }
    }
  }

  return state
}

@Composable
fun VideoPlayer(
  modifier: Modifier = Modifier,
  path: String
) {
  val transcodeRepository = remember { OnlineTranscodeRepository() }
  val uiState = rememberTranscodeState(
    videoPath = path,
    transcodeRepository = transcodeRepository
  )

}