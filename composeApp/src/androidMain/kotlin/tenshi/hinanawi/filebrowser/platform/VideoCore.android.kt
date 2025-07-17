package tenshi.hinanawi.filebrowser.platform

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
actual fun VideoCore(
  modifier: Modifier,
  url: String,
  onReady: () -> Unit,
  onError: (String) -> Unit
) {
  val context = LocalContext.current

  val exoPlayer = remember(url) {
    ExoPlayer.Builder(context).build().apply {
      val dataSourceFactory = DefaultHttpDataSource.Factory()
      val mediaSource = if (url.contains(".m3u8")) {
        HlsMediaSource.Factory(dataSourceFactory)
          .createMediaSource(MediaItem.fromUri(url))
      } else {
        null
      }

      if (url.contains(".m3u8")) {
        setMediaSource(mediaSource as HlsMediaSource)
      } else {
        setMediaItem(MediaItem.fromUri(url))
      }

      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          when (playbackState) {
            Player.STATE_READY -> onReady()
            else -> {}
          }
        }

        override fun onPlayerError(error: PlaybackException) {
          onError(error.message ?: "EXO播放器错误")
        }
      })

      prepare()
    }
  }

  DisposableEffect(exoPlayer) {
    onDispose {
      exoPlayer.release()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      PlayerView(context).apply {
        player = exoPlayer
        useController = true
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
    }
  )
}