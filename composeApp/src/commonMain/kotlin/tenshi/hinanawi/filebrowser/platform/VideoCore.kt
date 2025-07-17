package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoCore(
  modifier: Modifier = Modifier,
  url: String,
  onReady: () -> Unit,
  onError: (String) -> Unit
)