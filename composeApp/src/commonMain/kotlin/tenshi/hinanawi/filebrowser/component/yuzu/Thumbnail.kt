package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.data.repo.ThumbnailRepository
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.model.response.FileType

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

@Composable
fun Thumbnail(
  modifier: Modifier = Modifier,
  file: FileInfo,
  thumbnailRepository: ThumbnailRepository
) {
  if (file.type != FileType.Image && file.type != FileType.Video) {
    FileTypeIcon(
      modifier = modifier,
      fileType = file.type,
      iconSize = 48.dp
    )
    return
  }
  val thumbnailState = rememberThumbnailState(file.path, thumbnailRepository)
  when (val currentState = thumbnailState.value) {
    is ThumbnailState.Idle, ThumbnailState.Error -> FileTypeIcon(
      modifier = modifier,
      fileType = file.type,
      iconSize = 48.dp
    )

    is ThumbnailState.Loading -> CircularProgressIndicator(modifier = modifier.size(32.dp))
    is ThumbnailState.Success -> Image(
      bitmap = currentState.image,
      contentDescription = "${file.name}缩略图",
      modifier = modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
      contentScale = ContentScale.Crop
    )
  }
}
