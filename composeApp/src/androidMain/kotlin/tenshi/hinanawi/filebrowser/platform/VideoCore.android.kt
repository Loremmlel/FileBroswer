package tenshi.hinanawi.filebrowser.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.util.currentTimeMillis
import java.util.*
import kotlin.math.abs

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
  var seekIndicator by remember { mutableStateOf<SeekIndicator?>(null) }

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
  LaunchedEffect(Unit) {
    while (true) {
      currentPosition = exoPlayer.currentPosition
      duration = exoPlayer.duration.coerceAtLeast(0)
      delay(500)
    }
  }

  // 自动隐藏控制栏
  LaunchedEffect(exoPlayer.isPlaying, showControlsOverlay) {
    if (showControlsOverlay && exoPlayer.isPlaying) {
      delay(2000)
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
      seekIndicator = seekIndicator,
      onSeek = { exoPlayer.seekTo(it) },
      onPlayPause = {
        if (exoPlayer.isPlaying) {
          exoPlayer.pause()
          showControlsOverlay = true
        } else {
          exoPlayer.play()
        }
      },
      onSpeedBoost = {
        isSpeedBoosting = it
        exoPlayer.setPlaybackSpeed(if (isSpeedBoosting) 3f else 1f)
      },
      onClose = {
        onClose()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      },
      onShowControls = { showControlsOverlay = it },
      onSeekIndicatorChange = { seekIndicator = it }
    )
  } else {
    Box(modifier = modifier) {
      PlayerContent(
        exoPlayer = exoPlayer,
        isPlaying = exoPlayer.isPlaying,
        isSpeedBoosting = isSpeedBoosting,
        currentPosition = currentPosition,
        duration = duration,
        showControlsOverlay = showControlsOverlay,
        seekIndicator = seekIndicator,
        onPlayPause = {
          if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            showControlsOverlay = true
          } else {
            exoPlayer.play()
          }
        },
        onSeek = { exoPlayer.seekTo(it) },
        onSpeedBoost = {
          isSpeedBoosting = it
          exoPlayer.setPlaybackSpeed(if (isSpeedBoosting) 3f else 1f)
        },
        onFullscreen = {
          isFullscreen = true
          activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        },
        onClose = onClose,
        onShowControls = { showControlsOverlay = it },
        onSeekIndicatorChange = { seekIndicator = it },
        showControls = showControls
      )
    }
  }
}

data class SeekIndicator(
  val direction: SeekDirection,
  val seconds: Int
)

enum class SeekDirection {
  Forward, Backward
}

@Composable
private fun PlayerContent(
  exoPlayer: ExoPlayer,
  isPlaying: Boolean,
  isSpeedBoosting: Boolean,
  currentPosition: Long,
  duration: Long,
  showControlsOverlay: Boolean,
  seekIndicator: SeekIndicator?,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onSpeedBoost: (Boolean) -> Unit,
  onFullscreen: () -> Unit,
  onClose: (() -> Unit)?,
  onShowControls: (Boolean) -> Unit,
  onSeekIndicatorChange: (SeekIndicator?) -> Unit,
  showControls: Boolean
) {
  var dragStartX by remember { mutableFloatStateOf(0f) }
  var currentDragX by remember { mutableFloatStateOf(0f) }
  var isDragging by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()

  Box(modifier = Modifier.fillMaxSize()) {
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
          val longPressThreshold = 300L
          awaitEachGesture {
            awaitFirstDown()
            val downTime = currentTimeMillis()
            var isLongPress = false

            val longPressJob = scope.launch {
              delay(longPressThreshold)
              isLongPress = true
              onSpeedBoost(true)
            }

            val up = waitForUpOrCancellation()

            longPressJob.cancel()
            if (isLongPress) {
              onSpeedBoost(false)
            }
            if (up != null) {
              val pressDuration = currentTimeMillis() - downTime
              if (pressDuration < longPressThreshold) {
                onShowControls(true)
              }
            }
          }
        }
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragStart = { offset ->
              dragStartX = offset.x
              currentDragX = offset.x
              isDragging = true
            },
            onDragEnd = {
              if (!isDragging) {
                return@detectHorizontalDragGestures
              }
              val dragDistance = currentDragX - dragStartX

              val halfScreenWidth = size.width / 2f
              val ratio = dragDistance / halfScreenWidth

              val seekSeconds = (ratio * 300).toInt()

              if (abs(seekSeconds) > 0) {
                val newPosition = (currentPosition + seekSeconds * 1000)
                  .coerceIn(0, duration)
                Log.e("fuck", "$currentPosition -> $newPosition")
                onSeek(newPosition)
              }

              onSeekIndicatorChange(null)
              isDragging = false
            },
            onDragCancel = {
              onSeekIndicatorChange(null)
              isDragging = false
            },
            onHorizontalDrag = { change, dragAmount ->
              if (!isDragging) {
                return@detectHorizontalDragGestures
              }
              currentDragX += dragAmount
              val dragDistance = currentDragX - dragStartX

              val halfScreenWidth = size.width / 2f
              val ratio = dragDistance / halfScreenWidth

              val seekSeconds = (ratio * 300).toInt()

              if (abs(seekSeconds) > 0) {
                val direction = if (seekSeconds > 0) SeekDirection.Forward else SeekDirection.Backward
                onSeekIndicatorChange(SeekIndicator(direction, abs(seekSeconds)))
              } else {
                onSeekIndicatorChange(null)
              }
            }
          )
        }
    )

    // 显示快进/快退指示器
    seekIndicator?.let {
      SeekIndicatorOverlay(
        direction = it.direction,
        seconds = it.seconds,
        modifier = Modifier.align(Alignment.Center)
      )
    }

    // 显示三倍速指示器
    SpeedBoostIndicator(
      visible = isSpeedBoosting,
      modifier = Modifier.align(Alignment.Center)
    )

    // 显示控制界面
    if (showControls) {
      PlayerControlsOverlay(
        visible = showControlsOverlay,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        isSpeedBoosting = isSpeedBoosting,
        onPlayPause = onPlayPause,
        onFullscreen = onFullscreen,
        onClose = onClose,
        onControlsInteraction = onShowControls
      )
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
  seekIndicator: SeekIndicator?,
  onSeek: (Long) -> Unit,
  onPlayPause: () -> Unit,
  onSpeedBoost: (Boolean) -> Unit,
  onClose: (() -> Unit)?,
  onShowControls: (Boolean) -> Unit,
  onSeekIndicatorChange: (SeekIndicator?) -> Unit
) {
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
      PlayerContent(
        exoPlayer = exoPlayer,
        isPlaying = isPlaying,
        isSpeedBoosting = isSpeedBoosting,
        currentPosition = currentPosition,
        duration = duration,
        showControlsOverlay = true,
        seekIndicator = seekIndicator,
        onPlayPause = onPlayPause,
        onSeek = onSeek,
        onSpeedBoost = onSpeedBoost,
        onFullscreen = onExitFullscreen,
        onClose = onClose,
        onShowControls = onShowControls,
        onSeekIndicatorChange = onSeekIndicatorChange,
        showControls = true
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
  isSpeedBoosting: Boolean,
  onPlayPause: () -> Unit,
  onFullscreen: () -> Unit,
  onClose: (() -> Unit)?,
  onControlsInteraction: (Boolean) -> Unit
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
        .pointerInput(Unit) {
          detectTapGestures(
            // 点击空白区域隐藏控制界面
            onTap = {
              onControlsInteraction(false)
            }
          )
        }
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
            .pointerInput(Unit) {
              detectTapGestures { }
            }
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
            detectTapGestures { }
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
          .pointerInput(Unit) {
            detectTapGestures { }
          }
      ) {
        VideoProgressBar(
          currentPosition = currentPosition,
          duration = duration
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
) {
  var dragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(0f) }

  val progress = if (duration > 0) {
    if (dragging) dragPosition else currentPosition.toFloat() / duration
  } else 0f

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(16.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
        .align(Alignment.Center)
    )

    Box(
      modifier = Modifier
        .fillMaxWidth(progress)
        .height(2.dp)
        .background(Color.White, RoundedCornerShape(1.dp))
        .align(Alignment.CenterStart)
    )

    Box(
      modifier = Modifier
        .offset(x = (progress * (300.dp - 8.dp)))
        .size(8.dp)
        .background(Color.White, CircleShape)
        .align(Alignment.CenterStart)
    )
  }
}

@Composable
private fun SpeedBoostIndicator(
  modifier: Modifier = Modifier,
  visible: Boolean
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
  ) {
    val infiniteTransition = rememberInfiniteTransition(label = "speedBoostTransition")
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.2f,
      targetValue = 0.6f,
      animationSpec = infiniteRepeatable(
        animation = tween(800),
        repeatMode = RepeatMode.Reverse
      ),
      label = "speedBoostAlpha"
    )

    Box(
      modifier = Modifier
        .size(100.dp)
        .background(
          Color.Black.copy(alpha = alpha),
          RoundedCornerShape(12.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.FastForward,
          contentDescription = "三倍速",
          tint = Color.White.copy(alpha = 0.8f),
          modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "3x",
          color = Color.White.copy(alpha = 0.8f),
          style = MaterialTheme.typography.headlineSmall
        )
      }
    }
  }
}

@Composable
private fun SeekIndicatorOverlay(
  direction: SeekDirection,
  seconds: Int,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .width(100.dp)
      .wrapContentHeight()
      .background(
        Color.Black.copy(alpha = 0.6f),
        RoundedCornerShape(12.dp)
      )
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = if (direction == SeekDirection.Forward)
          Icons.Default.FastForward else Icons.Default.FastRewind,
        contentDescription = if (direction == SeekDirection.Forward) "快进" else "快退",
        tint = Color.White,
        modifier = Modifier.size(40.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${if (direction == SeekDirection.Forward) "+" else "-"}${seconds}秒",
        color = Color.White,
        style = MaterialTheme.typography.headlineSmall
      )
    }
  }
}

private fun formatTime(timeMillis: Long): String {
  val totalSeconds = timeMillis / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}