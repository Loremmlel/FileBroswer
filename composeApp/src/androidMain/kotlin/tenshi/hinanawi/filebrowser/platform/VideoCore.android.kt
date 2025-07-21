package tenshi.hinanawi.filebrowser.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.*

@SuppressLint("SourceLockedOrientationActivity")
@OptIn(UnstableApi::class)
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
  val context = LocalContext.current
  val activity = context as? Activity

  var isFullscreen by remember { mutableStateOf(false) }
  var showControlsOverlay by remember { mutableStateOf(true) }
  var currentPosition by remember { mutableLongStateOf(0L) }
  var duration by remember { mutableLongStateOf(0L) }
  var isSpeedBoosting by remember { mutableStateOf(false) }

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
            Player.STATE_READY -> {
              onReady()
            }
            else -> {}
          }
        }

        override fun onPlayerError(error: PlaybackException) {
          onError(error.message ?: "EXO播放器错误")
        }
      })

      playWhenReady = autoPlay
      prepare()
    }
  }

  // 更新播放进度
  LaunchedEffect(exoPlayer) {
    while (true) {
      currentPosition = exoPlayer.currentPosition
      duration = exoPlayer.duration.coerceAtLeast(0)
      delay(500)
    }
  }

  // 自动隐藏控制栏
  LaunchedEffect(exoPlayer) {
    if (showControlsOverlay && exoPlayer.isPlaying) {
      delay(3000)
      showControlsOverlay = false
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      exoPlayer.release()
    }
  }

  if (isFullscreen) {
    FullscreenPlayer(
      exoPlayer = exoPlayer,
      onExitFullscreen = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      },
      isPlaying = exoPlayer.isPlaying,
      isSpeedBoosting = isSpeedBoosting,
      currentPosition = currentPosition,
      duration = duration,
      onSeek = { exoPlayer.seekTo(it) },
      onPlayPause = {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
      },
      onSpeedBoost = {
        isSpeedBoosting = it
        exoPlayer.setPlaybackSpeed(if (isSpeedBoosting) 3f else 1f)
      },
      onClose = {
        onClose()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      }
    )
  } else {
    Box(modifier = modifier) {
      AndroidView(
        factory = { context ->
          PlayerView(context).apply {
            player = exoPlayer
            useController = false
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = {
                showControlsOverlay = !showControlsOverlay
              }
            )
          }
      )

      if (showControls) {
        PlayerControlsOverlay(
          visible = showControlsOverlay,
          isPlaying = exoPlayer.isPlaying,
          currentPosition = currentPosition,
          duration = duration,
          onPlayPause = {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
          },
          onSeek = { position -> exoPlayer.seekTo(position) },
          onFullscreen = {
            isFullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          },
          onClose = onClose,
          onSpeedBoost = { boost ->
            isSpeedBoosting = boost
            exoPlayer.setPlaybackSpeed(if (boost) 3f else 1f)
          },
          isSpeedBoosting = isSpeedBoosting
        )
      }
    }
  }
}

@Composable
private fun FullscreenPlayer(
  exoPlayer: ExoPlayer,
  onExitFullscreen: () -> Unit,
  isPlaying: Boolean,
  isSpeedBoosting: Boolean,
  currentPosition: Long,
  duration: Long,
  onSeek: (Long) -> Unit,
  onPlayPause: () -> Unit,
  onSpeedBoost: (Boolean) -> Unit,
  onClose: (() -> Unit)?
) {
  var showControls by remember { mutableStateOf(true) }

  Dialog(
    onDismissRequest = onExitFullscreen,
    properties = DialogProperties(
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
      usePlatformDefaultWidth = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      AndroidView(
        factory = { context ->
          PlayerView(context).apply {
            player = exoPlayer
            useController = false
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(onTap = { showControls = !showControls })
          }
      )

      PlayerControlsOverlay(
        visible = showControls,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        onPlayPause = onPlayPause,
        onSeek = onSeek,
        onFullscreen = onExitFullscreen,
        onClose = onClose,
        onSpeedBoost = onSpeedBoost,
        isSpeedBoosting = isSpeedBoosting
      )
    }
  }
}

@Composable
private fun PlayerControlsOverlay(
  visible: Boolean,
  isPlaying: Boolean,
  currentPosition: Long,
  duration: Long,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onFullscreen: () -> Unit,
  onClose: (() -> Unit)?,
  onSpeedBoost: (Boolean) -> Unit,
  isSpeedBoosting: Boolean
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.3f))
    ) {
      // 顶部控制栏
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .align(Alignment.TopStart),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        if (onClose != null) {
          IconButton(
            onClick = onClose,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "关闭视频",
              tint = Color.White
            )
          }
        }

        Spacer(Modifier.weight(1f))

        IconButton(
          onClick = onFullscreen,
          modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Fullscreen,
            contentDescription = "全屏",
            tint = Color.White
          )
        }
      }

      // 中央播放/暂停按钮
      IconButton(
        onClick = onPlayPause,
        modifier = Modifier
          .align(Alignment.Center)
          .size(64.dp)
          .background(Color.Black.copy(alpha = 0.5f), CircleShape)
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = {
                onPlayPause()
              },
              onLongPress = {
                onSpeedBoost(true)
              }
            )
          }
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPlaying) "暂停" else "播放",
          tint = Color.White,
          modifier = Modifier.size(32.dp)
        )
      }

      // 底部进度条和控制
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(
            Color.Black.copy(alpha = 0.5f),
            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
          )
          .padding(16.dp)
      ) {
        VideoProgressBar(
          currentPosition = currentPosition,
          duration = duration,
          onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = formatTime(currentPosition),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
          )

          if (isSpeedBoosting) {
            Text(
              text = "3x",
              color = Color.Yellow,
              style = MaterialTheme.typography.bodySmall
            )
          }

          Text(
            text = formatTime(duration),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
private fun VideoProgressBar(
  currentPosition: Long,
  duration: Long,
  onSeek: (Long) -> Unit
) {
  var dragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(0f) }

  val progress = if (duration > 0) {
    if (dragging) dragPosition else currentPosition.toFloat() / duration
  } else 0f

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(20.dp)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = {
            dragging = true
          },
          onDragEnd = {
            dragging = false
            if (duration > 0) {
              onSeek((dragPosition * duration).toLong())
            }
          },
          onDrag = { change, dragAmount ->
            dragPosition = (dragPosition + dragAmount.x / (size.width * 2)).coerceIn(0f, 1f)
            change.consume()
          }
        )
      }
  ) {
    // 背景进度条
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        .align(Alignment.Center)
    )

    // 当前进度
    Box(
      modifier = Modifier
        .fillMaxWidth(progress)
        .height(4.dp)
        .background(Color.Blue, RoundedCornerShape(2.dp))
        .align(Alignment.CenterStart)
    )

    // 拖拽手柄
    Box(
      modifier = Modifier
        .offset(x = (progress * (300.dp - 12.dp)))
        .size(12.dp)
        .background(Color.Red, CircleShape)
        .align(Alignment.CenterStart)
    )
  }
}

private fun formatTime(timeMillis: Long): String {
  val totalSeconds = timeMillis / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}