// desktopMain/kotlin/tenshi/hinanawi/filebrowser/platform/VideoCore.desktop.kt
package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

@Composable
actual fun VideoCore(
  modifier: Modifier,
  url: String,
  onReady: () -> Unit,
  onError: (String) -> Unit
) {
  val mediaPlayerComponent = remember {
    EmbeddedMediaPlayerComponent()
  }

  LaunchedEffect(url) {
    mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
          onReady()
        }

        override fun error(mediaPlayer: MediaPlayer) {
          onError("VLC播放器错误")
        }
      }
    )

    mediaPlayerComponent.mediaPlayer().media().play(url)
  }

  DisposableEffect(mediaPlayerComponent) {
    onDispose {
      mediaPlayerComponent.mediaPlayer().controls().stop()
      mediaPlayerComponent.release()
    }
  }

  SwingPanel(
    modifier = modifier,
    factory = {
      mediaPlayerComponent
    }
  )
}
