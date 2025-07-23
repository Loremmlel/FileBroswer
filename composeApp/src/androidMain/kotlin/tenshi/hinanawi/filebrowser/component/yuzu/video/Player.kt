package tenshi.hinanawi.filebrowser.component.yuzu.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AndroidVideoPlayer(
  private val context: Context,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : PlatformVideoPlayer {

  var exoPlayer: ExoPlayer? = null
    private set

  private val _state = MutableStateFlow(VideoPlayerState())
  override val state = _state.asStateFlow()

  private val _event = MutableSharedFlow<VideoPlayerEvent>()
  override val event = _event.asSharedFlow()

  private var positionUpdateJob: Job? = null

  @OptIn(UnstableApi::class)
  override fun initialize(url: String, autoPlay: Boolean) {
    release()
    exoPlayer = ExoPlayer.Builder(context).build().apply {
      val mediaSource = if (url.contains(".m3u8")) {
        HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
          .createMediaSource(MediaItem.fromUri(url))
      } else null
      if (mediaSource != null) {
        setMediaSource(mediaSource)
      } else {
        setMediaItem(MediaItem.fromUri(url))
      }

      addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
          scope.launch {
            _event.emit(VideoPlayerEvent.Error(error.message ?: "ExoPlayer错误"))
          }
          _state.value = _state.value.copy(error = error.message ?: "ExoPlayer错误")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          val isLoading = playbackState == Player.STATE_BUFFERING
          _state.value = _state.value.copy(isLoading = isLoading)

          if (playbackState == Player.STATE_READY) {
            _state.value = _state.value.copy(
              duration = duration.milliseconds
            )
          }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          _state.value = _state.value.copy(isPlaying = isPlaying)

          scope.launch {
            if (isPlaying) {
              _event.emit(VideoPlayerEvent.Play)
              startPositionUpdates()
            } else {
              _event.emit(VideoPlayerEvent.Pause)
              stopPositionUpdates()
            }
          }
        }
      })

      playWhenReady = autoPlay
      prepare()
    }
  }

  override fun play() {
    exoPlayer?.play()
  }

  override fun pause() {
    exoPlayer?.pause()
  }

  override fun seekTo(position: Duration) {
    exoPlayer?.seekTo(position.inWholeMilliseconds)
  }

  override fun setVolume(volume: Float) {
    exoPlayer?.volume = volume
    _state.value = _state.value.copy(volume = volume)
  }

  override fun setPlaybackSpeed(speed: Float) {
    exoPlayer?.setPlaybackSpeed(speed)
    _state.value = _state.value.copy(playbackSpeed = speed)
  }

  override fun release() {
    stopPositionUpdates()
    exoPlayer?.release()
    exoPlayer = null
  }

  private fun startPositionUpdates() {
    stopPositionUpdates()
    positionUpdateJob = scope.launch {
      while (true) {
        exoPlayer?.let { player ->
          _state.value = _state.value.copy(
            currentPosition = player.currentPosition.milliseconds
          )
        }
        delay(100)
      }
    }
  }

  private fun stopPositionUpdates() {
    positionUpdateJob?.cancel()
    positionUpdateJob = null
  }

}