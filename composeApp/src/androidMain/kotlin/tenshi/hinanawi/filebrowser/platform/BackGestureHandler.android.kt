package tenshi.hinanawi.filebrowser.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackGestureHandler(enabled: Boolean, onBack: () -> Unit) {
  BackHandler(enabled = enabled) {
    onBack()
  }
}