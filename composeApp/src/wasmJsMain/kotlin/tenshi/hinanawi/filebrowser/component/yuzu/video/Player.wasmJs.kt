package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@JsModule("hls.js")
external class Hls(config: HlsConfig = definedExternally) {
  fun attachMedia(video: HTMLVideoElement)
  fun loadSource(url: String)
  fun destroy()
  fun startLoad(startPosition: Double = definedExternally)
  fun recoverMediaError()
  fun on(event: String, callback: (event: JsAny, data: JsAny) -> Unit)

  companion object {
    fun isSupported(): Boolean

    val Events: HlsEvents
    val ErrorTypes: HlsErrorTypes
  }
}

external interface HlsConfig {
  var enableWorker: Boolean?
  var lowLatencyMode: Boolean?
  var backBufferLength: JsNumber?
}

external interface HlsEvents {
  val MEDIA_ATTACHED: String
  val MANIFEST_PARSED: String
  val ERROR: String
  val LEVEL_LOADED: String
  val FRAG_LOADED: String
}

external interface HlsErrorTypes {
  val NETWORK_ERROR: String
  val MEDIA_ERROR: String
  val OTHER_ERROR: String
}

external interface HlsError {
  val type: String
  val details: String
  val fatal: Boolean
}

@OptIn(ExperimentalComposeUiApi::class)
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
  val body = document.body!!
  val isMobile = remember { detectMobileDevice() }
  val controller = remember {
    VideoPlayerController(BrowserVideoPlayer())
  }

  LaunchedEffect(url) {
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

  val dialog = remember { document.createElement("dialog") as HTMLDialogElement }

//  // 处理全屏
//  LaunchedEffect(isFullscreen) {
//    val videoElement = (controller.platformPlayer as BrowserVideoPlayer).video
//    if (isFullscreen) {
//      videoElement.requestFullscreen()
//    } else {
//      if (document.fullscreen) {
//        document.exitFullscreen()
//      }
//    }
//  }

  DisposableEffect(Unit) {
    dialog.style.apply {
      width = "100vw"
      height = "100vh"
      backgroundColor = "transparent"
      border = "none"
      padding = "0"
      margin = "0"
    }

    // 等我有时间了，把这些组件用html标签一个个实现然后控制吧。
//    ComposeViewport(dialog) {
//      val playerState by controller.playerState.collectAsState()
//      val controlsState by controller.controlsState.collectAsState()
//      var isFullscreen by remember { mutableStateOf(false) }
//
//      LaunchedEffect(isFullscreen) {
//        val videoElement = (controller.platformPlayer as BrowserVideoPlayer).video
//        if (isFullscreen) {
//          videoElement.requestFullscreen()
//        } else {
//          if (document.fullscreen) {
//            document.exitFullscreen()
//          }
//        }
//      }
//
//      // 添加一个 Box 来为子组件提供对齐（align）的能力
//      Box(modifier = Modifier
//        .fillMaxSize()
//        .background(Color.Transparent)
//        .then(
//          if (isMobile) {
//            Modifier.rememberGestureEventHandler(controller)
//          } else {
//            Modifier.rememberKeyboardEventHandler(controller)
//          }
//        )
//      ) {
//        VideoControlsOverlay(
//          modifier = Modifier.background(Color.Transparent),
//          state = playerState.copy(isFullscreen = isFullscreen),
//          controlsState = controlsState,
//          title = title,
//          onPlayPause = { controller.handlePlayerEvent(VideoPlayerEvent.TogglePlayPause) },
//          onFullscreen = {
//            isFullscreen = !isFullscreen
//            controller.handlePlayerEvent(VideoPlayerEvent.ToggleFullscreen)
//          },
//          onClose = onClose,
//          onControlsClick = {
//            if (isMobile) {
//              controller.handlePlayerEvent(VideoPlayerEvent.ShowControls)
//            } else {
//              controller.handlePlayerEvent(VideoPlayerEvent.HideControls)
//            }
//          }
//        )
//
//        // 速度指示器
//        SpeedIndicator(
//          isVisible = controlsState.showSpeedIndicator,
//          speed = playerState.playbackSpeed,
//          modifier = Modifier.align(Alignment.Center)
//        )
//
//        // 跳转预览指示器
//        SeekPreviewIndicator(
//          isVisible = controlsState.showSeekPreview,
//          targetPosition = controlsState.seekPreviewPosition,
//          currentDuration = playerState.duration,
//          modifier = Modifier.align(Alignment.Center)
//        )
//
//        // 音量指示器
//        VolumeIndicator(
//          isVisible = controlsState.showVolumeIndicator,
//          volume = playerState.volume,
//          modifier = Modifier
//            .align(Alignment.TopEnd)
//            .padding(16.dp)
//        )
//
//        // 桌面端显示键盘帮助
//        if (!isMobile) {
//          KeyboardHelpOverlay(
//            modifier = Modifier
//              .align(Alignment.BottomStart)
//              .padding(16.dp)
//          )
//        }
//
//        // 加载指示器
//        if (playerState.isLoading) {
//          CircularProgressIndicator(
//            modifier = Modifier.align(Alignment.Center)
//          )
//        }
//      }
//    }

    body.appendChild(dialog)
    dialog.showModal()

    onDispose {
      dialog.close()
      dialog.remove()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
  ) {
    // Video element - 使用 HtmlView
    HtmlVideoView(
      player = controller.platformPlayer as BrowserVideoPlayer,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
private fun HtmlVideoView(
  player: BrowserVideoPlayer,
  modifier: Modifier = Modifier
) {
  DisposableEffect(Unit) {
    // 创建一个容器来放置 video 元素
    val container = document.createElement("div") as HTMLElement
    container.id = "video-player-container"
    container.style.cssText = """
      position: fixed;
      z-index: 1;
      background-color: black;
      overflow: hidden;
      pointer-events: none;
    """.trimIndent()

    // 获取 canvas 元素
    val canvas = document.querySelector("canvas") as HTMLCanvasElement?
    canvas?.parentElement?.insertBefore(container, canvas)

    container.appendChild(player.video)

    onDispose {
      container.remove()
    }
  }

  LaunchedEffect(Unit) {
    window.addEventListener("resize") {
      updateVideoContainerPosition()
    }

    // 初始更新
    delay(100) // 等待 Compose 布局完成
    updateVideoContainerPosition()
  }

  // 监听组件位置变化
  Box(
    modifier = modifier
      .onGloballyPositioned { coordinates ->
        updateVideoContainerPosition()
      }
  ) {
  }
}

private fun updateVideoContainerPosition() {
  js(
    """
    (function() {
      const container = document.getElementById('video-player-container');
      const canvas = document.querySelector('canvas');
      if (!container || !canvas) return;
      
      const canvasRect = canvas.getBoundingClientRect();
      
      // 让 video 容器完全覆盖 canvas
      container.style.left = canvasRect.left + 'px';
      container.style.top = canvasRect.top + 'px';
      container.style.width = canvasRect.width + 'px';
      container.style.height = canvasRect.height + 'px';
    })()
  """
  )
}

class BrowserVideoPlayer(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : PlatformVideoPlayer {
  val video: HTMLVideoElement = (document.createElement("video") as HTMLVideoElement).apply {
    style.cssText = """
      width: 100%;
      height: 100%;
      object-fit: contain;
      background-color: black;
      pointer-events: none;
    """.trimIndent()
    controls = true
    setAttribute("playsinline", "true")
    setAttribute("webkit-playsinline", "true")
    crossOrigin = "anonymous"
  }
  private var hlsInstance: Hls? = null

  private val _state = MutableStateFlow(VideoPlayerState())
  override val state = _state.asStateFlow()

  private val _event = MutableSharedFlow<VideoPlayerEvent>()
  override val event = _event.asSharedFlow()

  private var positionUpdateJob: Job? = null

  init {
    setupEventListeners()
  }

  override fun initialize(url: String, autoPlay: Boolean) {
    destroyHls()

    if (url.contains(".m3u8")) {
      if (video.canPlayType("application/vnd.apple.mpegurl") != CanPlayTypeResult.EMPTY) {
        video.src = url
      } else if (isHlsSupported()) {
        initializeHls(url, autoPlay)
        return
      } else {
        scope.launch {
          _event.emit(VideoPlayerEvent.Error("浏览器不支持 HLS 播放"))
        }
        return
      }
    } else {
      video.src = url
    }

    video.autoplay = autoPlay
  }

  @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
  private fun initializeHls(url: String, autoPlay: Boolean) {
    try {
      val config = newEmptyJsObject() as HlsConfig
      config.enableWorker = true
      config.lowLatencyMode = true
      config.backBufferLength = 90.toJsNumber()

      val hls = Hls(config)
      hlsInstance = hls

      hls.attachMedia(video)
      hls.on(Hls.Events.MEDIA_ATTACHED) { _, _ ->
        hls.loadSource(url)
      }
      hls.on(Hls.Events.MANIFEST_PARSED) { _, _ ->
        if (autoPlay) {
          scope.launch {
            play()
          }
        }
      }
      hls.on(Hls.Events.ERROR) { _, data ->
        val error = data as HlsError
        if (!error.fatal) {
          return@on
        }
        when (error.type) {
          Hls.ErrorTypes.NETWORK_ERROR -> {
            console.error("Fatal network error encountered, trying to recover")
            hls.startLoad()
          }

          Hls.ErrorTypes.MEDIA_ERROR -> {
            console.error("Fatal media error encountered, trying to recover")
            hls.recoverMediaError()
          }

          else -> {
            console.error("Fatal error, cannot recover")
            scope.launch {
              _event.emit(VideoPlayerEvent.Error("HLS 致命错误: ${error.details}"))
            }
            hls.destroy()
          }
        }
      }
    } catch (e: Exception) {
      scope.launch {
        _event.emit(VideoPlayerEvent.Error("初始化 HLS 失败: ${e.message}"))
      }
    }
  }

  private fun destroyHls() {
    hlsInstance?.destroy()
    hlsInstance = null
  }

  override fun play() {
    scope.launch {
      try {
        video.play().await()
      } catch (e: Exception) {
        _event.emit(VideoPlayerEvent.Error("播放失败: ${e.message}"))
      }
    }
  }

  override fun pause() {
    video.pause()
  }

  override fun seekTo(position: Duration) {
    val seconds = position.inWholeMilliseconds / 1000.0
    if (seconds.isFinite() && !seconds.isNaN()) {
      video.currentTime = seconds
    }
  }

  override fun setVolume(volume: Float) {
    video.volume = volume.toDouble()
    _state.value = _state.value.copy(volume = volume)
  }

  override fun setPlaybackSpeed(speed: Float) {
    video.playbackRate = speed.toDouble()
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  override fun release() {
    stopPositionUpdates()
    destroyHls()
    video.pause()
    video.remove()
  }

  private fun setupEventListeners() {
    video.addEventListener("play") {
      _state.value = _state.value.copy(isPlaying = true)
      startPositionUpdates()
    }

    video.addEventListener("pause") {
      _state.value = _state.value.copy(isPlaying = false)
      stopPositionUpdates()
    }

    video.addEventListener("ended") {
      _state.value = _state.value.copy(isPlaying = false)
      stopPositionUpdates()
    }

    video.addEventListener("loadstart") {
      _state.value = _state.value.copy(isLoading = true)
    }

    video.addEventListener("loadeddata") {
      _state.value = _state.value.copy(isLoading = false)
    }

    video.addEventListener("waiting") {
      _state.value = _state.value.copy(isLoading = true)
    }

    video.addEventListener("canplay") {
      _state.value = _state.value.copy(isLoading = false)
    }

    video.addEventListener("durationchange") {
      val duration = video.duration
      if (!duration.isNaN() && duration.isFinite()) {
        _state.value = _state.value.copy(
          duration = (duration * 1000).toLong().milliseconds
        )
      }
    }

    video.addEventListener("error") { event ->
      val errorMessage = when (video.error?.code?.toInt()) {
        1 -> "媒体资源获取被用户终止"
        2 -> "网络错误"
        3 -> "媒体解码错误"
        4 -> "媒体格式不支持"
        else -> "未知播放错误"
      }
      _state.value = _state.value.copy(error = errorMessage, isLoading = false)
      scope.launch { _event.emit(VideoPlayerEvent.Error(errorMessage)) }
    }
  }

  private fun startPositionUpdates() {
    stopPositionUpdates()
    positionUpdateJob = scope.launch {
      while (isActive) {
        val currentTime = video.currentTime
        if (!currentTime.isNaN() && currentTime.isFinite()) {
          _state.value = _state.value.copy(
            currentPosition = (currentTime * 1000).toLong().milliseconds
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

private fun isHlsSupported(): Boolean = js("typeof Hls !== 'undefined' && Hls.isSupported()")

private fun detectMobileDevice(): Boolean {
  val userAgent = window.navigator.userAgent.lowercase()
  val mobileKeywords = listOf(
    "android", "webos", "iphone", "ipad", "ipod", "blackberry",
    "windows phone", "mobile", "tablet"
  )

  val isMobileUserAgent = mobileKeywords.any { userAgent.contains(it) }
  val isSmallScreen = window.innerWidth <= 768

  return isMobileUserAgent || (hasTouchSupport() && isSmallScreen)
}

private fun hasTouchSupport(): Boolean = js("'ontouchstart' in window || navigator.maxTouchPoints > 0")

private external val console: Console

private external interface Console {
  fun error(message: String)
}

private fun newEmptyJsObject(): JsAny = js("{}")