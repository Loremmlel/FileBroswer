package tenshi.hinanawi.filebrowser.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent

@Composable
actual fun BackGestureHandler(enabled: Boolean, onBack: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .alpha(0f)
      .onKeyEvent {
        if (enabled && it.key == Key.Escape) {
          onBack()
          true
        } else {
          false
        }
      }
  )
}