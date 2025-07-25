package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.net.URLEncoder
import java.util.*
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
  val osName = remember {
    System.getProperty("os.name").lowercase(Locale.ENGLISH)
  }
  val isMac = "mac" in osName || "darwin" in osName
  val encodedUrl = encodeUrl(url)

  if (isMac) {
    MacExternalVideoPlayer(encodedUrl, onClose, onError)
  } else {
    EmbeddedVideoPlayer(modifier, encodedUrl, title, autoPlay, onError, onClose)
  }
}

@Composable
private fun MacExternalVideoPlayer(
  url: String,
  onClose: () -> Unit,
  onError: (String) -> Unit
) {
  var process by remember { mutableStateOf<Process?>(null) }
  LaunchedEffect(url) {
    try {
      val vlcPath = "/Applications/VLC.app/Contents/MacOS/VLC"
      val arguments = listOf(
        vlcPath, url,
        "--start-paused",
        "--no-macosx-autoplay"
      )
      val p = ProcessBuilder(arguments).start()
      process = p

      withContext(Dispatchers.IO) {
        p.waitFor()
      }
      onClose()
    } catch (e: Exception) {
      onError("无法启动VLC。请确保VLC已安装在 /Applications/VLC.app。错误: ${e.message}")
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      process?.destroyForcibly()
    }
  }

  // 显示一个占位符，直到VLC启动
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    Text(
      text = if (process != null)
        "已经打开VLC。如果看到playlist.m3u8，点击或者等待一会儿即可播放。"
      else
        "正在VLC中打开...",
      color = Color.White,
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.bodyLarge
    )
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EmbeddedVideoPlayer(
  modifier: Modifier,
  url: String,
  title: String,
  autoPlay: Boolean,
  onError: (String) -> Unit,
  onClose: () -> Unit
) {
  val mediaPlayerComponent = remember {
    EmbeddedMediaPlayerComponent().apply {
      isFocusable = false
      isEnabled = false
      isRequestFocusEnabled = false
      background = java.awt.Color(0, 0, 0, 0)
    }
  }
  val controller = remember {
    VideoPlayerController(DesktopVideoPlayer(mediaPlayerComponent))
  }

  LaunchedEffect(url) {
    // fix记录：这里双重编码url了，AI真不靠谱
    controller.initialize(url, autoPlay)
  }

  LaunchedEffect(controller) {
    controller.playerEvent.collect { event ->
      if (event is VideoPlayerEvent.Error) {
        onError(event.message)
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      controller.release()
    }
  }

  val playerState by controller.playerState.collectAsState()
  val controlsState by controller.controlsState.collectAsState()
  var isFullscreen by remember { mutableStateOf(false) } // 全屏状态应由外部窗口处理

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .rememberKeyboardEventHandler(controller)
  ) {
    // 有bug，swingPanel一直都在Compose组件之上，都好几年了还没修复。但勉强能看视频，没办法，等吧。
    SwingPanel(
      factory = {
        mediaPlayerComponent
      },
      modifier = Modifier
        .fillMaxSize()
        .focusable(false)
        .background(Color.Transparent)
    )

    VideoControlsOverlay(
      modifier = Modifier.background(Color.Transparent),
      state = playerState.copy(isFullscreen = isFullscreen),
      controlsState = controlsState,
      title = title,
      onPlayPause = { controller.handlePlayerEvent(VideoPlayerEvent.TogglePlayPause) },
      onFullscreen = {
        isFullscreen = !isFullscreen
        controller.handlePlayerEvent(VideoPlayerEvent.ToggleFullscreen)
      },
      onClose = onClose,
      onControlsClick = { controller.handlePlayerEvent(VideoPlayerEvent.ShowControls) }
    )

    SpeedIndicator(
      isVisible = controlsState.showSpeedIndicator,
      speed = playerState.playbackSpeed,
      modifier = Modifier.align(Alignment.Center)
    )

    SeekPreviewIndicator(
      isVisible = controlsState.showSeekPreview,
      targetPosition = controlsState.seekPreviewPosition,
      currentDuration = playerState.duration,
      modifier = Modifier.align(Alignment.Center)
    )

    VolumeIndicator(
      isVisible = controlsState.showVolumeIndicator,
      volume = playerState.volume,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
    )

    KeyboardHelpOverlay(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(16.dp)
    )

    if (playerState.isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}

class DesktopVideoPlayer(
  private val mediaPlayerComponent: EmbeddedMediaPlayerComponent,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : PlatformVideoPlayer {

  private val mediaPlayer: MediaPlayer = mediaPlayerComponent.mediaPlayer()

  private val _state = MutableStateFlow(VideoPlayerState())
  override val state = _state.asStateFlow()

  private val _event = MutableSharedFlow<VideoPlayerEvent>()
  override val event = _event.asSharedFlow()

  init {
    mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
      override fun playing(mediaPlayer: MediaPlayer) {
        _state.value = _state.value.copy(isPlaying = true, isLoading = false)
        scope.launch { _event.emit(VideoPlayerEvent.Play) }
      }

      override fun paused(mediaPlayer: MediaPlayer) {
        _state.value = _state.value.copy(isPlaying = false)
        scope.launch { _event.emit(VideoPlayerEvent.Pause) }
      }

      override fun stopped(mediaPlayer: MediaPlayer) {
        _state.value = _state.value.copy(isPlaying = false)
      }

      override fun finished(mediaPlayer: MediaPlayer) {
        _state.value = _state.value.copy(isPlaying = false, currentPosition = _state.value.duration)
      }

      override fun error(mediaPlayer: MediaPlayer) {
        val errorMsg = "VLC播放器遇到错误"
        _state.value = _state.value.copy(error = errorMsg, isLoading = false)
        scope.launch { _event.emit(VideoPlayerEvent.Error(errorMsg)) }
      }

      override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
        _state.value = _state.value.copy(currentPosition = newTime.milliseconds)
      }

      override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
        _state.value = _state.value.copy(duration = newLength.milliseconds)
      }

      override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
        _state.value = _state.value.copy(isLoading = newCache < 100f)
      }

      override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0f, 1f))
      }
    })
  }

  override fun initialize(url: String, autoPlay: Boolean) {
    val ok = mediaPlayer.media().play(url)
    if (!ok) {
      scope.launch { _event.emit(VideoPlayerEvent.Error("无法播放指定的URL: $url")) }
      return
    }
    if (!autoPlay) {
      mediaPlayer.controls().setPause(true)
    }
  }

  override fun play() {
    mediaPlayer.controls().play()
  }

  override fun pause() {
    mediaPlayer.controls().pause()
  }

  override fun seekTo(position: Duration) {
    mediaPlayer.controls().setTime(position.inWholeMilliseconds)
  }

  override fun setVolume(volume: Float) {
    mediaPlayer.audio().setVolume((volume * 100).toInt())
    _state.value = _state.value.copy(volume = volume)
  }

  override fun setPlaybackSpeed(speed: Float) {
    mediaPlayer.controls().setRate(speed)
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  override fun release() {
    mediaPlayerComponent.release()
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