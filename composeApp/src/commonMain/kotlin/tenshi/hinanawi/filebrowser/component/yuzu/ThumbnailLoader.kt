package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import tenshi.hinanawi.filebrowser.data.repo.ThumbnailRepository

sealed class ThumbnailState {
  object Loading : ThumbnailState()
  data class Success(val image: ImageBitmap) : ThumbnailState()
  object Error : ThumbnailState()
  object Idle : ThumbnailState()
}

@Composable
fun rememberThumbnailState(
  path: String,
  thumbnailRepository: ThumbnailRepository
): State<ThumbnailState> {
  val state = remember(path) { mutableStateOf<ThumbnailState>(ThumbnailState.Idle) }

  // 使用LaunchedEffect来在path变化时触发加载
  // 并且只在state为Idle时才开始加载，防止重组时重复加载
  LaunchedEffect(path) {
    if (state.value !is ThumbnailState.Idle) {
      return@LaunchedEffect
    }
    state.value = ThumbnailState.Loading
    val thumbnail = thumbnailRepository.getThumbnail(path)
    state.value = if (thumbnail != null) ThumbnailState.Success(thumbnail) else ThumbnailState.Error
  }

  return state
}