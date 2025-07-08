package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


object Toast {
  const val LENGTH_SHORT = 2000L
  const val LENGTH_LONG = 4000L

  fun makeText(message: String, duration: Long = LENGTH_SHORT): Toast {
    ToastManager.makeText(message, duration)
    return this
  }

  fun show() {
    ToastManager.show()
  }
}

data class ToastState(
  val message: String = "",
  val visible: Boolean = false,
  val duration: Long = Toast.LENGTH_SHORT
)

object ToastManager {
  private val _state = mutableStateOf(ToastState())
  val state: State<ToastState> = _state

  fun makeText(message: String, duration: Long = Toast.LENGTH_SHORT) {
    _state.value = ToastState(
      message = message,
      duration = duration
    )
  }

  fun show() {
    _state.value = _state.value.copy(visible = true)
  }

  fun hide() {
    _state.value = _state.value.copy(visible = false)
  }
}

@Composable
fun ToastContainer(
  content: @Composable () -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    content()
    val toastState by ToastManager.state

    AnimatedVisibility(
      visible = toastState.visible,
      enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 100.dp)
    ) {
      ToastContent(toastState.message)
    }

    LaunchedEffect(toastState.visible) {
      if (toastState.visible) {
        delay(toastState.duration)
        ToastManager.hide()
      }
    }
  }

}

@Composable
private fun ToastContent(
  message: String
) {
  Box(
    modifier = Modifier
      .fillMaxWidth(0.5f)
      .wrapContentHeight()
      .padding(8.dp)
      .background(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp)
      )
  ) {
    Text(
      text = message,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.align(Alignment.Center)
    )
  }
}