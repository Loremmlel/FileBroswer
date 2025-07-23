package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Duration

/**
 * 控制覆盖层
 */
@Composable
fun VideoControlsOverlay(
  state: VideoPlayerState,
  controlsState: ControlsState,
  title: String,
  onPlayPause: () -> Unit,
  onFullscreen: () -> Unit,
  onClose: () -> Unit,
  onControlsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = controlsState.isVisible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable { onControlsClick() }
    ) {
      // 上层控制栏
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(Color.Black.copy(alpha = 0.5f))
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "关闭",
            tint = Color.White
          )
        }

        Text(
          text = title,
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )

        IconButton(onClick = onFullscreen) {
          Icon(
            imageVector = if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            contentDescription = if (state.isFullscreen) "退出全屏" else "全屏",
            tint = Color.White
          )
        }
      }

      // 中层播放按钮
      Box(
        modifier = Modifier
          .size(80.dp)
          .align(Alignment.Center)
          .background(Color.Black.copy(alpha = 0.5f), CircleShape)
          .clickable { onPlayPause() },
        contentAlignment = Alignment.Center
      ) {
        PlayPauseButton(
          isPlaying = state.isPlaying,
          onClick = onPlayPause,
          size = 60
        )
      }

      // 下层控制栏
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.5f))
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        PlayPauseButton(
          isPlaying = state.isPlaying,
          onClick = onPlayPause,
          size = 36
        )

        Spacer(modifier = Modifier.width(16.dp))

        VideoProgressBar(
          currentPosition = state.currentPosition,
          duration = state.duration,
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        TimeDisplay(
          currentPosition = state.currentPosition,
          duration = state.duration
        )
      }
    }
  }
}

/**
 * 播放/暂停按钮
 */
@Composable
fun PlayPauseButton(
  isPlaying: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Int = 48
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.size(size.dp)
  ) {
    Icon(
      imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
      contentDescription = if (isPlaying) "暂停" else "播放",
      tint = Color.White,
      modifier = Modifier.size((size * 0.6).dp)
    )
  }
}

/**
 * 进度条组件
 */
@Composable
fun VideoProgressBar(
  currentPosition: Duration,
  duration: Duration,
  modifier: Modifier = Modifier
) {
  val progress = if (duration.inWholeMilliseconds > 0) {
    (currentPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Box(
    modifier = modifier
      .height(4.dp)
      .clip(RoundedCornerShape(2.dp))
  ) {
    // 背景条
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White.copy(alpha = 0.3f))
    )

    // 进度条
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(progress)
        .background(Color.White)
    )
  }
}

/**
 * 时间显示组件
 */
@Composable
fun TimeDisplay(
  currentPosition: Duration,
  duration: Duration,
  modifier: Modifier = Modifier
) {
  Text(
    text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
    color = Color.White,
    style = MaterialTheme.typography.bodySmall,
    modifier = modifier
  )
}

/**
 * 速度指示器
 */
@Composable
fun SpeedIndicator(
  isVisible: Boolean,
  speed: Float,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut(),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "${speed}x",
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium
      )
    }
  }
}

/**
 * 跳转预览指示器
 */
@Composable
fun SeekPreviewIndicator(
  isVisible: Boolean,
  targetPosition: Duration,
  currentDuration: Duration,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut(),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "${formatDuration(targetPosition)} / ${formatDuration(currentDuration)}",
        color = Color.White,
        style = MaterialTheme.typography.titleMedium
      )
    }
  }
}

/**
 * 音量指示器
 */
@Composable
fun VolumeIndicator(
  isVisible: Boolean,
  volume: Float,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically(),
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = when {
            volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
            volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
          },
          contentDescription = "音量",
          tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${(volume * 100).toInt()}%",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium
        )
      }
    }
  }
}

private fun formatDuration(duration: Duration): String {
  val totalSeconds = duration.inWholeSeconds
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()
  val secondsString = if (seconds < 10) "0$minutes" else seconds.toString()

  return if (hours > 0) {
    "$hours:${minutesString}:${secondsString}"
  } else {
    "${minutesString}:${secondsString}"
  }
}