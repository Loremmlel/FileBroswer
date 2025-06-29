package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class BrowseViewModel(
  private val filesRepository: FilesRepository
) : ViewModel() {
  val navigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow(BrowserUiState())
  val uiState = _uiState.asStateFlow()

  private val allImages: List<FileInfo> get() = uiState.value.files.filter { it.type == FileType.Image }

  fun getData() {
    loadFiles()
  }

  private fun loadFiles() {
    viewModelScope.launch {
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

  fun deleteFile(file: FileInfo) {
    viewModelScope.launch {
      filesRepository.deleteFile("${navigator.requestPath}/${file.name}")
      getData()
    }
  }

  fun openImagePreview(image: FileInfo) {
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
    val currentIndex = uiState.value.files.indexOf(uiState.value.previewItem)
    if (currentIndex < uiState.value.files.size - 1) {
      openImagePreview(uiState.value.files[currentIndex + 1])
    }
  }

  fun previousImagePreview() {
    val currentIndex = uiState.value.files.indexOf(uiState.value.previewItem)
    if (currentIndex > 0) {
      openImagePreview(uiState.value.files[currentIndex - 1])
    }
  }
}

data class BrowserUiState(
  val files: List<FileInfo> = emptyList(),
  val fileLoading: Boolean = false,
  val previewItem: FileInfo? = null
)