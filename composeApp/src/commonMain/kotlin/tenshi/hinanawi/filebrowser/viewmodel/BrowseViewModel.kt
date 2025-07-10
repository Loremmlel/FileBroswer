package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.toAddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.util.ErrorHandler
import tenshi.hinanawi.filebrowser.util.firstAfter
import tenshi.hinanawi.filebrowser.util.firstBefore

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModel(
  private val filesRepository: FilesRepository,
  private val favoriteRepository: FavoriteRepository
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
            .catch { e ->
              ErrorHandler.handleException(e)
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

  fun addFavorite(file: FileInfo, favoriteId: Long) = viewModelScope.launch {
    val result = favoriteRepository.addFileToFavorite(file.toAddFileToFavoriteRequest(), favoriteId)
    if (result) {
      Toast.makeText("添加收藏成功", Toast.SHORT).show()
    } else {
      Toast.makeText("添加收藏失败", Toast.SHORT).show()
    }
  }

  fun deleteFile(file: FileInfo) = viewModelScope.launch {
    filesRepository.deleteFile("${navigator.requestPath}/${file.name}")
    getData()
  }


  fun downloadFile(file: FileInfo) = viewModelScope.launch {
    val filePath = "${navigator.requestPath}/${file.name}"
    filesRepository.downloadFile(filePath, file.name)
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
      Toast.makeText("没有图片正在预览", Toast.VERY_SHORT).show()
      return
    }
    // 写反了，耻辱
    val nextImage = _uiState.value.files.firstAfter(currentImageIndex) { it.type == FileType.Image }
    if (nextImage != null) {
      openImagePreview(nextImage)
    } else {
      Toast.makeText("没有下一张图片了, 显示第一张", Toast.VERY_SHORT).show()
      openImagePreview(_firstImage)
    }
  }

  fun previousImagePreview() {
    val currentImageIndex = _uiState.value.files.indexOfFirst {
      it.name == _uiState.value.previewItem?.name
    }
    if (currentImageIndex == -1) {
      Toast.makeText("没有图片正在预览", Toast.VERY_SHORT).show()
      return
    }
    val previousImage = _uiState.value.files.firstBefore(currentImageIndex) { it.type == FileType.Image }
    if (previousImage != null) {
      openImagePreview(previousImage)
    } else {
      Toast.makeText("没有上一张图片了, 显示最后一张", Toast.VERY_SHORT).show()
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