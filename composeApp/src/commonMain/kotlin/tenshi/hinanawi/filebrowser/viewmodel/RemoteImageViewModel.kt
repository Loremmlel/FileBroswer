package tenshi.hinanawi.filebrowser.viewmodel

import androidx.compose.ui.graphics.painter.Painter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.ImageRepository

class RemoteImageViewModel(
  private val imageRepository: ImageRepository
) : ViewModel() {
  private val _imageLoadState = MutableStateFlow<ImageLoadState>(ImageLoadState.Loading)
  val image = _imageLoadState.asStateFlow()

  fun loadImage(path: String) {
    viewModelScope.launch {
      imageRepository.getImageStream(path)
        .catch {
          _imageLoadState.value = ImageLoadState.Error(it.message ?: "Unknown Error")
        }
        .collect {
          _imageLoadState.value = it
        }
    }
  }
}

sealed class ImageLoadState {
  object Loading : ImageLoadState()
  data class Progress(val value: Float) : ImageLoadState()
  data class Success(val painter: Painter) : ImageLoadState()
  data class Error(val message: String) : ImageLoadState()
}