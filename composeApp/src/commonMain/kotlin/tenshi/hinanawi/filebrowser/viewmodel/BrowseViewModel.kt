package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.util.ErrorHandler
import tenshi.hinanawi.filebrowser.util.firstAfter
import tenshi.hinanawi.filebrowser.util.firstBefore

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModel(
  private val filesRepository: FilesRepository
) : ViewModel() {
  val navigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow(BrowserUiState())
  val uiState = _uiState.asStateFlow()

  private val _currentPath = MutableStateFlow(navigator.requestPath)

  private val _firstImage
    get() = _uiState.value.files.firstOrNull {
      it.type == FileType.Image
    }

  private val _lastImage
    get() = _uiState.value.files.lastOrNull {
      it.type == FileType.Image
    }

  init {
    viewModelScope.launch {
      _currentPath
        .flatMapLatest {
        filesRepository.getFiles(navigator.requestPath)
          .onStart {
            _uiState.update {
              it.copy(fileLoading = true)
            }
          }
          .catch { exception ->
            ErrorHandler.handleException(exception)
            _uiState.update {
              it.copy(fileLoading = false)
            }
          }
      }
        .collect { files ->
          _uiState.update {
            it.copy(
              files = files,
              fileLoading = false
            )
          }
        }
    }
  }

  fun getData() {
    closeImagePreview()
    _currentPath.value = navigator.requestPath
  }

  fun deleteFile(file: FileInfo) {
    viewModelScope.launch {
      filesRepository.deleteFile("${navigator.requestPath}/${file.name}")
      getData()
    }
  }

  fun downloadFile(file: FileInfo) {
    viewModelScope.launch {
      try {
        val filePath = "${navigator.requestPath}/${file.name}"
        filesRepository.downloadFile(filePath, file.name)
      } catch (e: Exception) {
        ErrorHandler.handleException(e)
      }
    }
  }

  fun openImagePreview(image: FileInfo?) {
    _uiState.update {
      it.copy(
        previewItem = image
      )
    }
  }

  fun closeImagePreview() {
    _uiState.update {
      it.copy(
        previewItem = null
      )
    }
  }

  fun nextImagePreview() {
    val currentImageIndex = _uiState.value.files.indexOfFirst {
      it.name == _uiState.value.previewItem?.name
    }
    if (currentImageIndex == -1) {
      return
    }
    // 写反了，耻辱
    val nextImage = _uiState.value.files.firstAfter(currentImageIndex) { it.type == FileType.Image }
    if (nextImage != null) {
      openImagePreview(nextImage)
    } else {
      openImagePreview(_firstImage)
    }
  }

  fun previousImagePreview() {
    val currentImageIndex = _uiState.value.files.indexOfFirst {
      it.name == _uiState.value.previewItem?.name
    }
    if (currentImageIndex == -1) {
      return
    }
    val previousImage = _uiState.value.files.firstBefore(currentImageIndex) { it.type == FileType.Image }
    if (previousImage != null) {
      openImagePreview(previousImage)
    } else {
      openImagePreview(_lastImage)
    }
  }

  fun playVideo(video: FileInfo) {

  }
}

data class BrowserUiState(
  val files: List<FileInfo> = emptyList(),
  val fileLoading: Boolean = false,
  val previewItem: FileInfo? = null,
  val playingVideo: FileInfo? = null
)