package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * 平台特定播放器接口
 */
interface PlatformVideoPlayer {
  val state: StateFlow<VideoPlayerState>
  val event: SharedFlow<VideoPlayerEvent>

  fun initialize(url: String, autoPlay: Boolean)
  fun play()
  fun pause()
  fun seekTo(position: Duration)
  fun setVolume(volume: Float)
  fun setPlaybackSpeed(speed: Float)
  fun release()
}


@Composable
expect fun VideoPlayer(
  modifier: Modifier = Modifier,
  url: String,
  title: String,
  autoPlay: Boolean = true,
  showControls: Boolean = true,
  onError: (String) -> Unit,
  onClose: () -> Unit
)