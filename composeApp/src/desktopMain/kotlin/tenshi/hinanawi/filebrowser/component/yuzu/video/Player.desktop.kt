package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import tenshi.hinanawi.filebrowser.util.currentTimeMillis
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Canvas
import java.awt.Dimension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
  val controller = remember {
    val player = DesktopVideoPlayer()
    VideoPlayerController(player)
  }

  LaunchedEffect(url) {
    controller.initialize(url, autoPlay)
  }

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

  DisposableEffect(Unit) {
    onDispose {
      controller.release()
    }
  }

  if (isFullscreen) {
    DialogWindow(
      onCloseRequest = { isFullscreen = false },
      state = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 720.dp)
      ),
      title = title,
      content = {
        DesktopVideoPlayerContent(
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
    )
  } else {
    DesktopVideoPlayerContent(
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
private fun DesktopVideoPlayerContent(
  controller: VideoPlayerController,
  playerState: VideoPlayerState,
  controlsState: ControlsState,
  title: String,
  isFullscreen: Boolean,
  onFullscreenToggle: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val keyboardHandler = rememberKeyboardHandler(controller)

  Box(
    modifier = modifier.then(keyboardHandler)
  ) {
    // VLCJ视频渲染区域
    (controller.platformPlayer as? DesktopVideoPlayer)?.canvas?.let { canvas ->
      SwingPanel(
        background = Color.Black,
        modifier = Modifier.fillMaxSize(),
        factory = {
          canvas.apply {
            preferredSize = Dimension(1280, 720)
            minimumSize = Dimension(800, 600)
          }
        }
      )
    }

    // 控制覆盖层
    VideoControlsOverlay(
      state = playerState.copy(isFullscreen = isFullscreen),
      controlsState = controlsState,
      title = title,
      onPlayPause = { controller.handlePlayerEvent(VideoPlayerEvent.TogglePlayPause) },
      onFullscreen = onFullscreenToggle,
      onClose = onClose,
      onControlsClick = {
        if (controlsState.isVisible) {
          controller.handlePlayerEvent(VideoPlayerEvent.HideControls)
        } else {
          controller.handlePlayerEvent(VideoPlayerEvent.ShowControls)
        }
      }
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

    // 键盘操作提示（可选）
    if (controlsState.isVisible) {
      KeyboardHelpOverlay(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(16.dp)
      )
    }
  }
}

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
fun rememberKeyboardHandler(
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

  return Modifier
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

class DesktopVideoPlayer(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : PlatformVideoPlayer {
  private val factory = MediaPlayerFactory()
  var mediaPlayer: EmbeddedMediaPlayer? = null
    private set

  var canvas: Canvas? = null
    private set

  private val _state = MutableStateFlow(VideoPlayerState())
  override val state = _state.asStateFlow()

  private val _event = MutableSharedFlow<VideoPlayerEvent>()
  override val event = _event.asSharedFlow()

  private var positionUpdateJob: Job? = null

  override fun initialize(url: String, autoPlay: Boolean) {
    release()

    canvas = Canvas().apply {
      setSize(1280, 720)
    }

    mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer().apply {
      videoSurface().set(factory.videoSurfaces().newVideoSurface(canvas))

      events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun playing(mediaPlayer: MediaPlayer?) {
          _state.value = _state.value.copy(isPlaying = true, isLoading = false)
          startPositionUpdates()
        }

        override fun paused(mediaPlayer: MediaPlayer?) {
          _state.value = _state.value.copy(isPlaying = false)
          stopPositionUpdates()
        }

        override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
          _state.value = _state.value.copy(isLoading = newCache < 10f)
        }

        override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
          if (newLength > 0) {
            _state.value = _state.value.copy(duration = newLength.milliseconds)
          }
        }

        override fun error(mediaPlayer: MediaPlayer?) {
          scope.launch {
            _event.emit(VideoPlayerEvent.Error("VLC播放器错误"))
          }
          _state.value = _state.value.copy(error = "VLC播放器错误", isLoading = false)
        }
      })

      media().play(url)
      if (!autoPlay) {
        controls().pause()
      }
    }
  }

  override fun play() {
    mediaPlayer?.controls()?.play()
  }

  override fun pause() {
    mediaPlayer?.controls()?.pause()
  }

  override fun seekTo(position: Duration) {
    mediaPlayer?.controls()?.setTime(position.inWholeMilliseconds)
  }

  override fun setVolume(volume: Float) {
    val vlcVolume = (volume * 100).toInt().coerceIn(0, 100)
    mediaPlayer?.audio()?.setVolume(vlcVolume)
    _state.value = _state.value.copy(volume = volume)
  }

  override fun setPlaybackSpeed(speed: Float) {
    mediaPlayer?.controls()?.setRate(speed)
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  override fun release() {
    stopPositionUpdates()
    mediaPlayer?.controls()?.stop()
    mediaPlayer?.release()
    mediaPlayer = null
    canvas = null
  }

  private fun startPositionUpdates() {
    stopPositionUpdates()
    positionUpdateJob = scope.launch {
      while (true) {
        mediaPlayer?.let { player ->
          val currentTime = player.status().time()
          _state.value = _state.value.copy(
            currentPosition = currentTime.milliseconds
          )
        }
        delay(100)
      }
    }
  }

  private fun stopPositionUpdates() {
    positionUpdateJob?.cancel()
    positionUpdateJob = null
  }
}