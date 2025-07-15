package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun BackGestureHandler(enabled: Boolean, onBack: () -> Unit) {
  val backCallback = { event: Event ->
    event.preventDefault()
    onBack()
  }

  DisposableEffect(enabled, onBack) {
    if (enabled) {
      window.history.pushState(null, "", window.location.href)
      window.addEventListener("popstate", backCallback)
    }
    onDispose {
      window.removeEventListener("popstate", backCallback)
    }
  }
}