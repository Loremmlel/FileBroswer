package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.catch
import tenshi.hinanawi.filebrowser.data.repo.ImageRepository
import tenshi.hinanawi.filebrowser.util.ErrorHandler

sealed class RemoteImageState {
  object Idle : RemoteImageState()
  object Loading : RemoteImageState()
  data class Progress(val value: Float) : RemoteImageState()
  data class Success(val painter: Painter) : RemoteImageState()
  data class Error(val message: String) : RemoteImageState()
}

@Composable
fun rememberRemoteImageState(
  path: String,
  imageRepository: ImageRepository
): State<RemoteImageState> {
  val state = remember(path) { mutableStateOf<RemoteImageState>(RemoteImageState.Idle) }
  LaunchedEffect(path) {
    if (state.value !is RemoteImageState.Idle) {
      return@LaunchedEffect
    }
    state.value = RemoteImageState.Loading

    imageRepository.getImageStream(path)
      .catch { e ->
        state.value = RemoteImageState.Error(e.message ?: "未知错误")
        ErrorHandler.handleException(e)
      }
      .collect {
        state.value = it
      }
  }
  return state
}

@Composable
fun RemoteImage(
  path: String,
  imageRepository: ImageRepository,
  modifier: Modifier = Modifier,
  contentDescription: String = "远程图片资源"
) {
  val imageState by rememberRemoteImageState(path, imageRepository)
  Box(modifier) {
    when (val currentState = imageState) {
      is RemoteImageState.Idle -> Unit
      is RemoteImageState.Loading ->
        CircularProgressIndicator(
          modifier = modifier
            .size(40.dp)
            .align(Alignment.Center)
        )

      is RemoteImageState.Progress ->
        LinearProgressIndicator(
          progress = { currentState.value },
          Modifier.fillMaxWidth()
        )

      is RemoteImageState.Error -> Unit

      is RemoteImageState.Success ->
        Image(
          painter = currentState.painter,
          contentDescription = contentDescription,
          modifier = modifier.fillMaxSize(),
          contentScale = ContentScale.Fit
        )
    }
  }
}