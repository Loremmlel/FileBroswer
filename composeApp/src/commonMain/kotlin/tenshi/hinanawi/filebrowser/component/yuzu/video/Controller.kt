package tenshi.hinanawi.filebrowser.component.yuzu.video

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VideoPlayerController(
  val platformPlayer: PlatformVideoPlayer,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
  private val _controlsState = MutableStateFlow(ControlsState())
  val controlsState = _controlsState.asStateFlow()

  private val _gestureEvent = MutableSharedFlow<GestureEvent>()
  val gestureEvent = _gestureEvent.asSharedFlow()

  private val _keyboardEvent = MutableSharedFlow<KeyboardEvent>()
  val keyboardEvent = _keyboardEvent.asSharedFlow()

  val playerState = platformPlayer.state
  val playerEvent = platformPlayer.event

  private var hideControlsJob: Job? = null

  init {
    scope.launch {
      playerEvent.collect { event ->
        when (event) {
          is VideoPlayerEvent.Play -> {
            scheduleHideControls()
          }

          is VideoPlayerEvent.Pause -> {
            cancelHideControls()
          }

          else -> {}
        }
      }
    }

    scope.launch {
      gestureEvent.collect { event ->
        processGestureEvent(event)
      }
    }

    scope.launch {
      keyboardEvent.collect { event ->
        processKeyboardEvent(event)
      }
    }
  }

  fun initialize(url: String, autoPlay: Boolean) {
    platformPlayer.initialize(url, autoPlay)
  }

  fun handlePlayerEvent(event: VideoPlayerEvent) {
    when (event) {
      is VideoPlayerEvent.Play -> platformPlayer.play()
      is VideoPlayerEvent.Pause -> platformPlayer.pause()
      is VideoPlayerEvent.TogglePlayPause -> {
        if (playerState.value.isPlaying) {
          platformPlayer.pause()
        } else {
          platformPlayer.play()
        }
      }

      is VideoPlayerEvent.SeekTo -> platformPlayer.seekTo(event.position)
      is VideoPlayerEvent.SetVolume -> platformPlayer.setVolume(event.volume)
      is VideoPlayerEvent.SetPlaybackSpeed -> platformPlayer.setPlaybackSpeed(event.speed)
      is VideoPlayerEvent.ShowControls -> showControls()
      is VideoPlayerEvent.HideControls -> hideControls()
      is VideoPlayerEvent.ToggleFullscreen -> toggleFullscreen()
      is VideoPlayerEvent.Error -> {}
    }
  }

  fun handleGestureEvent(event: GestureEvent) {
    scope.launch {
      _gestureEvent.emit(event)
    }
  }

  fun handleKeyboardEvent(event: KeyboardEvent) {
    scope.launch {
      _keyboardEvent.emit(event)
    }
  }

  private fun processGestureEvent(event: GestureEvent) {
    when (event) {
      is GestureEvent.LongPress -> {
        _controlsState.value = _controlsState.value.copy(
          showSpeedIndicator = event.isPressed
        )
        if (event.isPressed) {
          platformPlayer.setPlaybackSpeed(3.0f)
          hideControls()
        } else {
          platformPlayer.setPlaybackSpeed(1.0f)
        }
      }

      is GestureEvent.SwipeStart -> {
        _controlsState.value = _controlsState.value.copy(
          seekStartPosition = platformPlayer.state.value.currentPosition
        )
      }

      is GestureEvent.SwipePreview -> {
        _controlsState.value = _controlsState.value.copy(
          showSeekPreview = true,
          seekPreviewPosition = (_controlsState.value.seekStartPosition + event.offset).coerceIn(
            Duration.ZERO,
            platformPlayer.state.value.duration
          )
        )
      }

      is GestureEvent.VolumeAdjust -> {
        val newVolume = (playerState.value.volume - event.deltaY)
          .coerceIn(0f, 1f)
        platformPlayer.setVolume(newVolume)
        _controlsState.value = _controlsState.value.copy(showVolumeIndicator = true)

        // 2秒后隐藏音量指示器
        scope.launch {
          delay(2000)
          _controlsState.value = _controlsState.value.copy(showVolumeIndicator = false)
        }
      }

      is GestureEvent.SwipeEnd -> {
        _controlsState.value = _controlsState.value.copy(
          showSeekPreview = false
        )
        platformPlayer.seekTo(_controlsState.value.seekPreviewPosition)
      }
    }
  }

  private fun processKeyboardEvent(event: KeyboardEvent) {
    when (event) {
      is KeyboardEvent.LongPressRight -> {
        _controlsState.value = _controlsState.value.copy(showSpeedIndicator = event.isPressed)
        if (event.isPressed) {
          platformPlayer.setPlaybackSpeed(3.0f)
        } else {
          platformPlayer.setPlaybackSpeed(1.0f)
        }
      }

      is KeyboardEvent.FastForward -> {
        val newPosition = playerState.value.currentPosition + 10.seconds
        platformPlayer.seekTo(newPosition.coerceAtMost(playerState.value.duration))
      }

      is KeyboardEvent.FastRewind -> {
        val newPosition = playerState.value.currentPosition - 10.seconds
        platformPlayer.seekTo(newPosition.coerceAtLeast(Duration.ZERO))
      }

      is KeyboardEvent.VolumeChange -> {
        val newVolume = (playerState.value.volume + event.delta).coerceIn(0f, 1f)
        platformPlayer.setVolume(newVolume)
        _controlsState.value = _controlsState.value.copy(showVolumeIndicator = true)

        // 2秒后隐藏音量指示器
        scope.launch {
          delay(2000)
          _controlsState.value = _controlsState.value.copy(showVolumeIndicator = false)
        }
      }
    }
  }

  private fun showControls() {
    cancelHideControls()
    _controlsState.value = _controlsState.value.copy(isVisible = true)
    if (playerState.value.isPlaying) {
      scheduleHideControls()
    }
  }

  private fun hideControls() {
    _controlsState.value = _controlsState.value.copy(isVisible = false)
    cancelHideControls()
  }

  private fun toggleFullscreen() {
    // 平台特定实现将处理全屏逻辑
  }

  private fun scheduleHideControls() {
    cancelHideControls()
    hideControlsJob = scope.launch {
      delay(2000)
      if (playerState.value.isPlaying) {
        hideControls()
      }
    }
  }

  private fun cancelHideControls() {
    hideControlsJob?.cancel()
    hideControlsJob = null
  }

  fun release() {
    platformPlayer.release()
  }
}