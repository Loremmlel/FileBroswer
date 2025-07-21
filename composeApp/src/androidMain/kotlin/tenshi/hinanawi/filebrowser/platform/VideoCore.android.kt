package tenshi.hinanawi.filebrowser.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.util.currentTimeMillis
import java.util.*
import kotlin.math.abs

data class SeekIndicator(val direction: SeekDirection, val seconds: Int)
enum class SeekDirection { Forward, Backward }

data class PlayerActions(
  val exoPlayer: ExoPlayer,
  val onPlayPause: () -> Unit,
  val onSeek: (Long) -> Unit,
  val onSpeedBoost: (Boolean) -> Unit,
  val onShowControls: (Boolean) -> Unit,
  val onSeekIndicatorChange: (SeekIndicator?) -> Unit
)

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
@Composable
actual fun VideoCore(
  modifier: Modifier,
  url: String,
  autoPlay: Boolean,
  showControls: Boolean,
  onReady: () -> Unit,
  onError: (String) -> Unit,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? Activity

  var isFullscreen by remember { mutableStateOf(false) }
  var showControlsOverlay by remember { mutableStateOf(true) }
  var currentPosition by remember { mutableLongStateOf(0L) }
  var duration by remember { mutableLongStateOf(0L) }
  var isSpeedBoosting by remember { mutableStateOf(false) }
  var seekIndicator by remember { mutableStateOf<SeekIndicator?>(null) }

  val exoPlayer = remember(url) {
    ExoPlayer.Builder(context).build().apply {
      val mediaSource = if (url.contains(".m3u8")) {
        HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
          .createMediaSource(MediaItem.fromUri(url))
      } else null

      if (mediaSource != null) setMediaSource(mediaSource)
      else setMediaItem(MediaItem.fromUri(url))

      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_READY) onReady()
        }

        override fun onPlayerError(error: PlaybackException) {
          onError(error.message ?: "EXO播放器错误")
        }
      })

      playWhenReady = autoPlay
      prepare()
    }
  }

  // 更新播放进度和自动隐藏控制栏
  LaunchedEffect(Unit) {
    while (true) {
      currentPosition = exoPlayer.currentPosition
      duration = exoPlayer.duration.coerceAtLeast(0)
      delay(500)
    }
  }

  LaunchedEffect(exoPlayer.isPlaying, showControlsOverlay) {
    if (showControlsOverlay && exoPlayer.isPlaying) {
      delay(2000)
      showControlsOverlay = false
    }
  }

  DisposableEffect(Unit) {
    onDispose { exoPlayer.release() }
  }

  val playerActions = PlayerActions(
    exoPlayer = exoPlayer,
    onPlayPause = {
      if (exoPlayer.isPlaying) {
        exoPlayer.pause()
        showControlsOverlay = true
      } else {
        exoPlayer.play()
      }
    },
    onSeek = { exoPlayer.seekTo(it) },
    onSpeedBoost = {
      isSpeedBoosting = it
      exoPlayer.setPlaybackSpeed(if (it) 3f else 1f)
    },
    onShowControls = { showControlsOverlay = it },
    onSeekIndicatorChange = { seekIndicator = it }
  )

  if (isFullscreen) {
    Dialog(
      onDismissRequest = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      },
      properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
      )
    ) {
      Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        PlayerContent(
          playerActions = playerActions,
          isPlaying = exoPlayer.isPlaying,
          isSpeedBoosting = isSpeedBoosting,
          currentPosition = currentPosition,
          duration = duration,
          showControlsOverlay = showControlsOverlay,
          seekIndicator = seekIndicator,
          showControls = showControls,
          onFullscreen = {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          },
          onClose = {
            onClose()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          }
        )
      }
    }
  } else {
    Box(modifier = modifier) {
      PlayerContent(
        playerActions = playerActions,
        isPlaying = exoPlayer.isPlaying,
        isSpeedBoosting = isSpeedBoosting,
        currentPosition = currentPosition,
        duration = duration,
        showControlsOverlay = showControlsOverlay,
        seekIndicator = seekIndicator,
        showControls = showControls,
        onFullscreen = {
          isFullscreen = true
          activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        },
        onClose = onClose
      )
    }
  }
}

@Composable
private fun PlayerContent(
  playerActions: PlayerActions,
  isPlaying: Boolean,
  isSpeedBoosting: Boolean,
  currentPosition: Long,
  duration: Long,
  showControlsOverlay: Boolean,
  seekIndicator: SeekIndicator?,
  showControls: Boolean,
  onFullscreen: () -> Unit,
  onClose: (() -> Unit)?
) {
  val scope = rememberCoroutineScope()
  var dragState by remember { mutableStateOf(DragState()) }

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      factory = { context ->
        PlayerView(context).apply {
          player = playerActions.exoPlayer
          useController = false
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown()
            val downTime = currentTimeMillis()
            var isLongPress = false

            val longPressJob = scope.launch {
              delay(300L)
              isLongPress = true
              playerActions.onSpeedBoost(true)
            }

            val up = waitForUpOrCancellation()
            longPressJob.cancel()

            if (isLongPress) {
              playerActions.onSpeedBoost(false)
            } else if (up != null && currentTimeMillis() - downTime < 300L) {
              playerActions.onShowControls(true)
            }
          }
        }
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragStart = { offset ->
              dragState = DragState(startX = offset.x, currentX = offset.x, isDragging = true)
            },
            onDragEnd = {
              if (dragState.isDragging) {
                val seekSeconds = calculateSeekSeconds(dragState, size.width)
                if (abs(seekSeconds) > 0) {
                  val newPosition = (playerActions.exoPlayer.currentPosition + seekSeconds * 1000)
                    .coerceIn(0, duration)
                  playerActions.onSeek(newPosition)
                }
              }
              playerActions.onSeekIndicatorChange(null)
              dragState = DragState()
            },
            onDragCancel = {
              playerActions.onSeekIndicatorChange(null)
              dragState = DragState()
            },
            onHorizontalDrag = { _, dragAmount ->
              if (dragState.isDragging) {
                dragState = dragState.copy(currentX = dragState.currentX + dragAmount)
                val seekSeconds = calculateSeekSeconds(dragState, size.width)

                if (abs(seekSeconds) > 0) {
                  val direction = if (seekSeconds > 0) SeekDirection.Forward else SeekDirection.Backward
                  playerActions.onSeekIndicatorChange(SeekIndicator(direction, abs(seekSeconds)))
                } else {
                  playerActions.onSeekIndicatorChange(null)
                }
              }
            }
          )
        }
    )

    // 指示器叠加层
    seekIndicator?.let {
      IndicatorOverlay(
        icon = if (it.direction == SeekDirection.Forward) Icons.Default.FastForward else Icons.Default.FastRewind,
        text = "${if (it.direction == SeekDirection.Forward) "+" else "-"}${it.seconds}秒",
        modifier = Modifier.align(Alignment.Center)
      )
    }

    AnimatedVisibility(
      visible = isSpeedBoosting,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      IndicatorOverlay(
        icon = Icons.Default.FastForward,
        text = "3x",
        animated = true
      )
    }

    // 控制界面
    if (showControls) {
      PlayerControlsOverlay(
        visible = showControlsOverlay,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        onPlayPause = playerActions.onPlayPause,
        onFullscreen = onFullscreen,
        onClose = onClose,
        onControlsInteraction = playerActions.onShowControls
      )
    }
  }
}

data class DragState(
  val startX: Float = 0f,
  val currentX: Float = 0f,
  val isDragging: Boolean = false
)

private fun calculateSeekSeconds(dragState: DragState, screenWidth: Int): Int {
  val dragDistance = dragState.currentX - dragState.startX
  val ratio = dragDistance / (screenWidth / 2f)
  return (ratio * 300).toInt()
}

@Composable
private fun IndicatorOverlay(
  icon: ImageVector,
  text: String,
  modifier: Modifier = Modifier,
  animated: Boolean = false
) {
  val alpha = if (animated) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicatorTransition")
    infiniteTransition.animateFloat(
      initialValue = 0.2f,
      targetValue = 0.6f,
      animationSpec = infiniteRepeatable(
        animation = tween(800),
        repeatMode = RepeatMode.Reverse
      ),
      label = "indicatorAlpha"
    ).value
  } else 0.6f

  Box(
    modifier = modifier
      .size(100.dp)
      .background(Color.Black.copy(alpha = alpha), RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.size(40.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = text,
        color = Color.White.copy(alpha = 0.8f),
        style = MaterialTheme.typography.headlineSmall
      )
    }
  }
}

@Composable
private fun PlayerControlsOverlay(
  visible: Boolean,
  isPlaying: Boolean,
  currentPosition: Long,
  duration: Long,
  onPlayPause: () -> Unit,
  onFullscreen: () -> Unit,
  onClose: (() -> Unit)?,
  onControlsInteraction: (Boolean) -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.3f))
        .pointerInput(Unit) {
          detectTapGestures { onControlsInteraction(false) }
        }
    ) {
      // 顶部控制栏
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .align(Alignment.TopStart),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        onClose?.let {
          ControlButton(Icons.Default.Close, "关闭视频", it)
        }
        Spacer(Modifier.weight(1f))
        ControlButton(Icons.Default.Fullscreen, "全屏", onFullscreen)
      }

      // 中央播放/暂停按钮
      ControlButton(
        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = if (isPlaying) "暂停" else "播放",
        onClick = onPlayPause,
        modifier = Modifier
          .align(Alignment.Center)
          .size(64.dp),
        iconSize = 32.dp
      )

      // 底部进度条
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(
            Color.Black.copy(alpha = 0.5f),
            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
          )
          .padding(16.dp)
          .pointerInput(Unit) { detectTapGestures { } }
      ) {
        VideoProgressBar(currentPosition, duration)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = formatTime(currentPosition),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
          )
          Text(
            text = formatTime(duration),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
private fun ControlButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = 24.dp
) {
  IconButton(
    onClick = onClick,
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.5f), CircleShape)
      .pointerInput(Unit) { detectTapGestures { } }
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier = Modifier.size(iconSize)
    )
  }
}

@Composable
private fun VideoProgressBar(currentPosition: Long, duration: Long) {
  val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

  Box(modifier = Modifier
    .fillMaxWidth()
    .height(16.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
        .align(Alignment.Center)
    )
    Box(
      modifier = Modifier
        .fillMaxWidth(progress)
        .height(2.dp)
        .background(Color.White, RoundedCornerShape(1.dp))
        .align(Alignment.CenterStart)
    )
    Box(
      modifier = Modifier
        .offset(x = progress * (300.dp - 8.dp))
        .size(8.dp)
        .background(Color.White, CircleShape)
        .align(Alignment.CenterStart)
    )
  }
}

private fun formatTime(timeMillis: Long): String {
  val totalSeconds = timeMillis / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}