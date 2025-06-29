package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class BrowseViewModel(
  private val filesRepository: FilesRepository
) : ViewModel() {
  val navigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow(BrowserUiState())
  val uiState = _uiState.asStateFlow()

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
          println("files: $files")
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

  fun imagePreview() {

  }
}

data class BrowserUiState(
  val files: List<FileInfo> = emptyList(),
  val fileLoading: Boolean = false
)