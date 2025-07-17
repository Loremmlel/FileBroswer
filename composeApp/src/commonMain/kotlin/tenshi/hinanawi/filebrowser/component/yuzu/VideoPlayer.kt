package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.runtime.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
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
  val pollingJob = remember { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(videoPath) {
    if (state.value !is TranscodeState.Idle) {
      return@LaunchedEffect
    }
    try {
      state.value = TranscodeState.Loading
      val startStatus = transcodeRepository.startTranscode(videoPath)
      state.value = TranscodeState.InProgress(startStatus)
    } catch (e: Exception) {
      state.value = TranscodeState.Error("$e - message: ${e.message}")
    }
  }

  LaunchedEffect(state) {
    when (val currentState = state.value) {
      is TranscodeState.InProgress -> {
        pollingJob.value?.cancel()
        pollingJob.value = launch {
          transcodeRepository.observeTranscode(currentState.status.id)
            .catch { e ->
              state.value = TranscodeState.Error("$e - message: ${e.message}")
            }
            .filterNotNull()
            .collect {
              state.value = if (it.status == TranscodeStatus.Enum.Completed && it.progress >= 0.99) {
                TranscodeState.Completed(it)
              } else {
                TranscodeState.InProgress(it)
              }
            }
        }
      }
      is TranscodeState.Completed -> {
        pollingJob.value?.cancel()
        pollingJob.value = null
      }
      else -> Unit
    }
  }

  DisposableEffect(videoPath) {
    onDispose {
      pollingJob.value?.cancel()
      scope.launch {
        val status = state.value
        (status as? TranscodeState.InProgress)?.let {
          transcodeRepository.stopTranscode(it.status.id)
        }
      }
    }
  }

  return state
}