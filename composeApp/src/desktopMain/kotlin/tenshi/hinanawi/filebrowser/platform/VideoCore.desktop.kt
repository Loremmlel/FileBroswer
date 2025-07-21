package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent


data class DesktopPlayerActions(
  override val player: MediaPlayer,
  override val onPlayPause: () -> Unit,
  override val onSeek: (Long) -> Unit,
  override val onSpeedBoost: (Boolean) -> Unit,
  override val onSeekIndicatorChange: (SeekIndicator?) -> Unit,
  val onVolumeChange: (Int) -> Unit
) : PlayerActions()

@Composable
actual fun VideoCore(
  modifier: Modifier,
  url: String,
  autoPlay: Boolean,
  showControls: Boolean,
  onReady: () -> Unit,
  onError: (String) -> Unit,
  onClose: () -> Unit
) {
  var isFullscreen by remember { mutableStateOf(false) }
  var currentPosition by remember { mutableLongStateOf(0L) }
  var duration by remember { mutableLongStateOf(0L) }
  var isSpeedBoosting by remember { mutableStateOf(false) }
  var seekIndicator by remember { mutableStateOf<SeekIndicator?>(null) }
  var volume by remember { mutableIntStateOf(50) }
  var isPlaying by remember { mutableStateOf(false) }

  val mediaPlayerComponent = remember { EmbeddedMediaPlayerComponent() }
  val mediaPlayer = remember { mediaPlayerComponent.mediaPlayer() }

  LaunchedEffect(url) {
    mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
      override fun playing(mediaPlayer: MediaPlayer) {
        isPlaying = true
        onReady()
      }

      override fun paused(mediaPlayer: MediaPlayer) {
        isPlaying = false
      }

      override fun stopped(mediaPlayer: MediaPlayer) {
        isPlaying = false
      }

      override fun error(mediaPlayer: MediaPlayer) {
        onError("VLCJ播放器错误")
      }

      override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
        duration = newLength
      }

      override fun positionChanged(mediaPlayer: MediaPlayer, newPosition: Float) {
        currentPosition = (duration * newPosition).toLong()
      }
    })
    mediaPlayer.media().play(url)
    mediaPlayer.audio().setVolume(volume)
    if (!autoPlay) {
      mediaPlayer.controls().pause()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      mediaPlayer.controls().pause()
      mediaPlayer.release()
    }
  }

  val playerActions = DesktopPlayerActions(
    player = mediaPlayer,
    onPlayPause = {
      if (mediaPlayer.status().isPlaying) {
        mediaPlayer.controls().pause()
      } else {
        mediaPlayer.controls().play()
      }
    },
    onSeek = {
      mediaPlayer.controls().setTime(it)
    },
    onSpeedBoost = {
      isSpeedBoosting = it
      mediaPlayer.controls().setRate(if (it) 3f else 0f)
    },
    onSeekIndicatorChange = {
      seekIndicator = it
    },
    onVolumeChange = {
      mediaPlayer.audio().setVolume(it.coerceIn(0, 100))
    }
  )

  //TODO
}
