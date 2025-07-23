package tenshi.hinanawi.filebrowser.component.yuzu.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
}