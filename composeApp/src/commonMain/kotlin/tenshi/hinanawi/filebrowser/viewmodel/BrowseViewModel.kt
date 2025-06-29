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
        .distinctUntilChanged()
        .onStart {
          _uiState.update {
            it.copy(loading = true)
          }
          println("uiState: ${_uiState.value}")
        }
        .catch { exception ->
          ErrorHandler.handleException(exception)
          _uiState.update {
            it.copy(loading = false)
          }
          println("uiState: ${_uiState.value}")
        }
        .collect { files ->
          println("files: $files")
          _uiState.update {
            it.copy(
              files = files,
              loading = false
            )
          }
          println("uiState: ${_uiState.value}")
        }
    }
  }

  fun deleteFile(file: FileInfo) {

  }
}

data class BrowserUiState(
  val files: List<FileInfo> = emptyList(),
  val loading: Boolean = false
)