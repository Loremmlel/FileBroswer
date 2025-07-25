package tenshi.hinanawi.filebrowser.component.yuzu.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SourceLockedOrientationActivity")
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
  val activity = context as? Activity

  val playerView = remember(context) {
    PlayerView(context).apply {
      useController = false
    }
  }

  val controller = remember(context) {
    val player = AndroidVideoPlayer(context)
    VideoPlayerController(player)
  }

  LaunchedEffect(url) {
    controller.initialize(url, autoPlay)
  }

  LaunchedEffect(controller.platformPlayer as AndroidVideoPlayer) {
    playerView.player = controller.platformPlayer.exoPlayer
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
    if (isFullscreen) {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
        playerView = playerView,
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
      playerView = playerView,
      onFullscreenToggle = { isFullscreen = !isFullscreen },
      onClose = onClose,
      modifier = modifier.fillMaxSize()
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
  playerView: PlayerView,
  onFullscreenToggle: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    // ExoPlayer视图
    AndroidView(
      factory = { context ->
        playerView
      },
      modifier = Modifier
        .fillMaxSize()
        .rememberGestureEventHandler(controller)
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

class AndroidVideoPlayer(
  private val context: Context,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : PlatformVideoPlayer {

  var exoPlayer: ExoPlayer? = null
    private set

  private val _state = MutableStateFlow(VideoPlayerState())
  override val state = _state.asStateFlow()

  private val _event = MutableSharedFlow<VideoPlayerEvent>()
  override val event = _event.asSharedFlow()

  private var positionUpdateJob: Job? = null

  @OptIn(UnstableApi::class)
  override fun initialize(url: String, autoPlay: Boolean) {
    release()
    exoPlayer = ExoPlayer.Builder(context).build().apply {
      val mediaSource = if (url.contains(".m3u8")) {
        HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
          .createMediaSource(MediaItem.fromUri(url))
      } else null
      if (mediaSource != null) {
        setMediaSource(mediaSource)
      } else {
        setMediaItem(MediaItem.fromUri(url))
      }

      addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
          scope.launch {
            _event.emit(VideoPlayerEvent.Error(error.message ?: "ExoPlayer错误"))
          }
          _state.value = _state.value.copy(error = error.message ?: "ExoPlayer错误")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          val isLoading = playbackState == Player.STATE_BUFFERING
          _state.value = _state.value.copy(isLoading = isLoading)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
          if (!timeline.isEmpty) {
            val window = Timeline.Window()
            timeline.getWindow(0, window)
            if (window.durationMs > 0) {
              _state.value = _state.value.copy(
                duration = window.durationMs.milliseconds
              )
            }
          }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          _state.value = _state.value.copy(isPlaying = isPlaying)

          scope.launch {
            if (isPlaying) {
              startPositionUpdates()
            } else {
              stopPositionUpdates()
            }
          }
        }
      })

      playWhenReady = autoPlay
      prepare()
    }
  }

  override fun play() {
    exoPlayer?.play()
  }

  override fun pause() {
    exoPlayer?.pause()
  }

  override fun seekTo(position: Duration) {
    exoPlayer?.seekTo(position.inWholeMilliseconds)
  }

  override fun setVolume(volume: Float) {
    exoPlayer?.volume = volume
    _state.value = _state.value.copy(volume = volume)
  }

  override fun setPlaybackSpeed(speed: Float) {
    exoPlayer?.setPlaybackSpeed(speed)
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  override fun release() {
    stopPositionUpdates()
    exoPlayer?.release()
    exoPlayer = null
  }

  private fun startPositionUpdates() {
    stopPositionUpdates()
    positionUpdateJob = scope.launch {
      while (true) {
        exoPlayer?.let { player ->
          _state.value = _state.value.copy(
            currentPosition = player.currentPosition.milliseconds
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