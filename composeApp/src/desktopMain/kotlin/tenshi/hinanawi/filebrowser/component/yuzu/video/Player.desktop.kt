package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.util.currentTimeMillis
import java.net.URLEncoder


@Composable
private fun KeyboardHelpOverlay(
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(
        Color.Black.copy(alpha = 0.7f),
        RoundedCornerShape(8.dp)
      )
      .padding(12.dp)
  ) {
    Text(
      text = "键盘操作:",
      color = Color.White,
      style = MaterialTheme.typography.labelMedium
    )
    Text(
      text = "空格: 播放/暂停",
      color = Color.White.copy(alpha = 0.8f),
      style = MaterialTheme.typography.bodySmall
    )
    Text(
      text = "←/→: 快退/快进",
      color = Color.White.copy(alpha = 0.8f),
      style = MaterialTheme.typography.bodySmall
    )
    Text(
      text = "↑/↓: 音量调节",
      color = Color.White.copy(alpha = 0.8f),
      style = MaterialTheme.typography.bodySmall
    )
    Text(
      text = "F: 全屏切换",
      color = Color.White.copy(alpha = 0.8f),
      style = MaterialTheme.typography.bodySmall
    )
    Text(
      text = "长按→: 3倍速播放",
      color = Color.White.copy(alpha = 0.8f),
      style = MaterialTheme.typography.bodySmall
    )
  }
}

@Composable
private fun Modifier.rememberKeyboardEventHandler(
  controller: VideoPlayerController
): Modifier {
  val scope = rememberCoroutineScope()
  val focusRequester = remember { FocusRequester() }

  var isRightPressed by remember { mutableStateOf(false) }
  var rightPressJob by remember { mutableStateOf<Job?>(null) }
  var rightPressStartTime by remember { mutableLongStateOf(currentTimeMillis()) }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  return this
    .focusRequester(focusRequester)
    .onKeyEvent { keyEvent ->
      when (keyEvent.type) {
        KeyEventType.KeyDown -> {
          when (keyEvent.key) {
            Key.Spacebar -> {
              controller.handlePlayerEvent(VideoPlayerEvent.TogglePlayPause)
              true
            }

            Key.DirectionLeft -> {
              controller.handleKeyboardEvent(KeyboardEvent.FastRewind)
              true
            }

            Key.DirectionRight -> {
              if (!isRightPressed) {
                isRightPressed = true
                rightPressStartTime = currentTimeMillis()
                rightPressJob = scope.launch {
                  delay(200)
                  controller.handleKeyboardEvent(KeyboardEvent.LongPressRight(true))
                }
              }
              true
            }

            Key.DirectionUp -> {
              controller.handleKeyboardEvent(KeyboardEvent.VolumeChange(0.1f))
              true
            }

            Key.DirectionDown -> {
              controller.handleKeyboardEvent(KeyboardEvent.VolumeChange(-0.1f))
              true
            }

            Key.F -> {
              controller.handlePlayerEvent(VideoPlayerEvent.ToggleFullscreen)
              true
            }

            Key.Escape -> {
              controller.handlePlayerEvent(VideoPlayerEvent.HideControls)
              true
            }

            else -> false
          }
        }

        KeyEventType.KeyUp -> {
          when (keyEvent.key) {
            Key.DirectionRight -> {
              if (isRightPressed) {
                isRightPressed = false
                rightPressJob?.cancel()
                controller.handleKeyboardEvent(
                  if (currentTimeMillis() - rightPressStartTime <= 200)
                    KeyboardEvent.FastForward
                  else
                    KeyboardEvent.LongPressRight(false)
                )
                rightPressJob = null
              }
              true
            }

            else -> false
          }
        }

        else -> false
      }
    }
}


private fun encodeUrl(url: String): String = try {
  val parts = url.split("?", limit = 2)
  if (parts.size == 2) {
    val pathQuery = parts[1].split("=")
    "${parts[0]}?${pathQuery[0]}=${URLEncoder.encode(pathQuery[1], "UTF-8")}"
  } else {
    url
  }
} catch (e: Exception) {
  println("编码URL时出现错误: ${e.message}")
  url
}