package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.SERVER_URL
import tenshi.hinanawi.filebrowser.data.repo.OnlineTranscodeRepository
import tenshi.hinanawi.filebrowser.data.repo.TranscodeRepository
import tenshi.hinanawi.filebrowser.model.response.TranscodeStatus
import tenshi.hinanawi.filebrowser.platform.VideoCore

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
  path: String,
  supportHevc: Boolean
) {
  if (supportHevc) {
    VideoCore(
      modifier = modifier
        .fillMaxSize(),
      url = "$SERVER_URL/video/direct?path=$path",
      onReady = {

      },
      onError = { message ->

      }
    )
  } else {
    val transcodeRepository = remember { OnlineTranscodeRepository() }
    val transcodeUiState = rememberTranscodeState(
      videoPath = path,
      transcodeRepository = transcodeRepository
    )
    var taskId by remember { mutableStateOf<String?>(null) }
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
      val state = transcodeUiState.value
      when (state) {
        is TranscodeUiState.Idle -> {
          Text(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "准备中...",
            style = MaterialTheme.typography.titleLarge
          )
        }

        is TranscodeUiState.Loading -> {
          Text(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "请求转码中...",
            style = MaterialTheme.typography.titleLarge
          )
        }

        is TranscodeUiState.InProgress -> {
          taskId = state.status.id
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp, horizontal = 24.dp)
              .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "转码进度: ${(state.status.progress * 100).toInt()}%",
              style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
              progress = { state.status.progress.toFloat() }
            )
          }
        }
        is TranscodeUiState.Completed -> {
          taskId = state.status.id
          Text(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "转码完成!",
            style = MaterialTheme.typography.titleLarge
          )
        }
        is TranscodeUiState.Error -> {
          Text(
            modifier = Modifier.align(Alignment.Center),
            text = "错误: ${state.message}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Red
          )
        }
      }
      if (state is TranscodeUiState.InProgress || state is TranscodeUiState.Completed) {
        VideoCore(
          modifier = modifier
            .fillMaxSize(),
          url = "$SERVER_URL/video/$taskId/playlist.m3u8",
          onReady = {

          },
          onError = { message ->

          }
        )
      }
    }
  }
}