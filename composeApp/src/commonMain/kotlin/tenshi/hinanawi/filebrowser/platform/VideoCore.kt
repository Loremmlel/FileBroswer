package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoCore(
  modifier: Modifier = Modifier,
  url: String,
  autoPlay: Boolean = true,
  showControls: Boolean = true,
  onReady: () -> Unit,
  onError: (String) -> Unit,
  onClose: () -> Unit
)

enum class SeekDirection {
  Forward, Backward
}

data class SeekIndicator(val direction: SeekDirection, val seconds: Int)

abstract class PlayerActions {
  abstract val player: Any
  abstract val onPlayPause: () -> Unit
  abstract val onSeek: (Long) -> Unit
  abstract val onSpeedBoost: (Boolean) -> Unit
  abstract val onSeekIndicatorChange: (SeekIndicator?) -> Unit
}