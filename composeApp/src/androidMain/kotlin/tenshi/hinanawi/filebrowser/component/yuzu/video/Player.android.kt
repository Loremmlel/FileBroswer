package tenshi.hinanawi.filebrowser.component.yuzu.video

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
actual fun VideoPlayer(
  modifier: Modifier,
  url: String,
  title: String,
  autoPlay: Boolean,
  showControls: Boolean,
  onError: (String) -> Unit,
  onClose: () -> Unit
) {
  val context = LocalContext.current

  val player = remember(url) {
    AndroidVideoPlayer(context)
  }
  val controller = remember(player) {
    VideoPlayerController(player)
  }

  LaunchedEffect(url) {
    controller.initialize(url, autoPlay)
  }

  // 监听错误事件
  LaunchedEffect(Unit) {
    controller.playerEvent.collect { event ->
      if (event is VideoPlayerEvent.Error) {
        onError(event.message)
      }
    }
  }

  val playerState by controller.playerState.collectAsState()
  val controlsState by controller.controlsState.collectAsState()

  var isFullscreen by remember { mutableStateOf(false) }

  // 处理全屏切换
  LaunchedEffect(isFullscreen) {
    val activity = context as? Activity
    if (isFullscreen) {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      controller.release()
    }
  }

  if (isFullscreen) {
    Dialog(
      onDismissRequest = { isFullscreen = false },
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
      )
    ) {
      VideoPlayerContent(
        controller = controller,
        playerState = playerState,
        controlsState = controlsState,
        title = title,
        isFullscreen = true,
        onFullscreenToggle = { isFullscreen = !isFullscreen },
        onClose = onClose,
        modifier = Modifier.fillMaxSize()
      )
    }
  } else {
    VideoPlayerContent(
      controller = controller,
      playerState = playerState,
      controlsState = controlsState,
      title = title,
      isFullscreen = false,
      onFullscreenToggle = { isFullscreen = !isFullscreen },
      onClose = onClose,
      modifier = modifier
    )
  }
}

@Composable
private fun VideoPlayerContent(
  controller: VideoPlayerController,
  playerState: VideoPlayerState,
  controlsState: ControlsState,
  title: String,
  isFullscreen: Boolean,
  onFullscreenToggle: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  var accumulatedOffset by remember { mutableStateOf(Offset.Zero) }
  var startPosition by remember { mutableStateOf(Duration.ZERO) }
  var isHorizontalDrag by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    // ExoPlayer视图
    AndroidView(
      factory = { context ->
        PlayerView(context).apply {
          player = (controller.player as AndroidVideoPlayer).exoPlayer
          useController = false
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { offset ->
              startPosition = playerState.currentPosition
              accumulatedOffset = Offset.Zero
            },
            onDrag = { change, dragAmount ->
              accumulatedOffset += Offset(dragAmount.x, dragAmount.y)

              val totalWidth = size.width.toFloat()
              val totalHeight = size.height.toFloat()

              // 识别主要拖动方向
              if (!isHorizontalDrag) {
                isHorizontalDrag = abs(accumulatedOffset.x) > abs(accumulatedOffset.y)
              }

              if (isHorizontalDrag) {
                // 水平移动：时间跳转
                val maxDuration = 5.minutes
                val deltaMs = (accumulatedOffset.x / totalWidth) * maxDuration.inWholeMilliseconds
                val newPosition = (startPosition.inWholeMilliseconds + deltaMs).toLong().milliseconds
                  .coerceIn(Duration.ZERO, playerState.duration)

                controller.handleGestureEvent(
                  GestureEvent.SwipePreview(newPosition)
                )
              } else {
                // 垂直移动：音量调节
                val volumeChange = accumulatedOffset.y / totalHeight * 0.5f
                controller.handleGestureEvent(
                  GestureEvent.VolumeAdjust(volumeChange)
                )
              }
            },
            onDragEnd = {
              when {
                // 水平滑动结束：执行跳转
                isHorizontalDrag -> {
                  controller.handleGestureEvent(
                    GestureEvent.SwipeEnd(
                      targetPosition = controlsState.seekPreviewPosition
                    )
                  )
                }
                // 垂直滑动结束：显示音量指示器
                accumulatedOffset.y != 0f -> {
                  controller.handleGestureEvent(
                    GestureEvent.VolumeAdjust(accumulatedOffset.y)
                  )
                }
              }
              accumulatedOffset = Offset.Zero
              isHorizontalDrag = false
            }
          )
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onLongPress = {
              controller.handleGestureEvent(GestureEvent.LongPress(true))
            },
            onTap = {
              if (!controlsState.isVisible) {
                controller.handlePlayerEvent(VideoPlayerEvent.ShowControls)
              }
            }
          )
        }
    )

    // 控制覆盖层
    VideoControlsOverlay(
      state = playerState.copy(isFullscreen = isFullscreen),
      controlsState = controlsState,
      title = title,
      onPlayPause = { controller.handlePlayerEvent(VideoPlayerEvent.TogglePlayPause) },
      onFullscreen = onFullscreenToggle,
      onClose = onClose,
      onControlsClick = { controller.handlePlayerEvent(VideoPlayerEvent.HideControls) }
    )

    // 速度指示器
    SpeedIndicator(
      isVisible = controlsState.showSpeedIndicator,
      speed = playerState.playbackSpeed,
      modifier = Modifier.align(Alignment.Center)
    )

    // 跳转预览指示器
    SeekPreviewIndicator(
      isVisible = controlsState.showSeekPreview,
      targetPosition = controlsState.seekPreviewPosition,
      currentDuration = playerState.duration,
      modifier = Modifier.align(Alignment.Center)
    )

    // 音量指示器
    VolumeIndicator(
      isVisible = controlsState.showVolumeIndicator,
      volume = playerState.volume,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
    )

    // 加载指示器
    if (playerState.isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}