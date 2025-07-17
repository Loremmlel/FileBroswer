package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import org.w3c.dom.CanPlayTypeResult
import org.w3c.dom.EMPTY
import org.w3c.dom.HTMLVideoElement

@JsModule("hls.js")
external class Hls(config: JsAny = definedExternally) {
  companion object {
    fun isSupported(): Boolean
  }

  fun loadSource(url: String)
  fun attachMedia(videoElement: HTMLVideoElement)
  fun destroy()

  fun on(event: String, callback: (event: String, data: JsAny) -> Unit)

  object Events {
    val MANIFEST_PARSED: String
    val ERROR: String
    val MEDIA_ATTACHED: String
  }
}

@Composable
actual fun VideoCore(
  modifier: Modifier,
  url: String,
  onReady: () -> Unit,
  onError: (String) -> Unit
) {
  val videoElement = remember {
    (document.createElement("video") as HTMLVideoElement).apply {
      style.apply {
        width = "100%"
        height = "100%"
      }
    }
  }
  var hls by remember { mutableStateOf<Hls?>(null) }

  LaunchedEffect(url, videoElement) {
    hls?.destroy()
    if (url.contains(".m3u8") && Hls.isSupported()) {
      val hlsInstance = Hls().apply {
        on(Hls.Events.MANIFEST_PARSED) { _, _ ->
          onReady()
        }
        on(Hls.Events.ERROR) { _, data ->
          onError("hls.js播放器错误: $data")
        }
        loadSource(url)
        attachMedia(videoElement)
      }
      hls = hlsInstance
    } else if (videoElement.canPlayType("application/vnd.apple.mpegurl") != CanPlayTypeResult.EMPTY) {
      // Safari原生支持Hls
      videoElement.src = url
      videoElement.addEventListener("loadedmetadata") {
        onReady()
      }
      videoElement.addEventListener("error") {
        onError("hls.js播放器错误")
      }
    } else {
      videoElement.src = url
      videoElement.addEventListener("loadedmetadata") {
        onReady()
      }
      videoElement.addEventListener("error") {
        onError("video元素错误")
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      hls?.destroy()
    }
  }

  document.body?.appendChild(videoElement)
}