package tenshi.hinanawi.filebrowser.component.yuzu.video

import kotlin.time.Duration

/**
 * 播放器状态
 */
data class VideoPlayerState(
  val isPlaying: Boolean = false,
  val currentPosition: Duration = Duration.ZERO,
  val duration: Duration = Duration.ZERO,
  val isLoading: Boolean = false,
  val volume: Float = 1.0f,
  val playbackSpeed: Float = 1.0f,
  val isFullscreen: Boolean = false,
  val error: String? = null
)

/**
 * 控制层状态管理
 */
data class ControlsState(
  val isVisible: Boolean = true,
  val isAnimating: Boolean = false,
  val showSpeedIndicator: Boolean = false,
  val showSeekPreview: Boolean = false,
  val seekPreviewPosition: Duration = Duration.ZERO,
  val showVolumeIndicator: Boolean = false
)

/**
 * 播放器事件
 */
sealed class VideoPlayerEvent {
  object Play : VideoPlayerEvent()
  object Pause : VideoPlayerEvent()
  object TogglePlayPause : VideoPlayerEvent()
  data class SeekTo(val position: Duration) : VideoPlayerEvent()
  data class SetVolume(val volume: Float) : VideoPlayerEvent()
  data class SetPlaybackSpeed(val speed: Float) : VideoPlayerEvent()
  object ToggleFullscreen : VideoPlayerEvent()
  object ShowControls : VideoPlayerEvent()
  object HideControls : VideoPlayerEvent()
  data class Error(val message: String) : VideoPlayerEvent()
}

/**
 * 手势事件
 */
sealed class GestureEvent {
  data class LongPress(val isPressed: Boolean) : GestureEvent()
  data class SwipePreview(val targetPosition: Duration) : GestureEvent()
  data class VolumeAdjust(val deltaY: Float) : GestureEvent()
  object SwipeEnd : GestureEvent()
}

/**
 * 键盘事件
 */
sealed class KeyboardEvent {
  data class LongPressRight(val isPressed: Boolean) : KeyboardEvent()
  object FastForward : KeyboardEvent()
  object FastRewind : KeyboardEvent()
  data class VolumeChange(val delta: Float) : KeyboardEvent()
}