package tenshi.hinanawi.filebrowser.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
    val navigator = BreadCrumbNavigator()

    private val _files = MutableStateFlow(emptyList<FileInfo>())
    val files get() = _files.asStateFlow()

    private var _loading = mutableStateOf(true)
    val loading: State<Boolean> = _loading

    fun getData() {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            filesRepository.getFiles(navigator.requestPath)
                .onStart {
                    _loading.value = true
                }
                .catch {
                    ErrorHandler.handleException(it)
                    _loading.value = false
                }
                .onEach {
                    _files.value = it
                }
                .onCompletion {
                    _loading.value = false
                }
                .collect()
        }
    }

    fun deleteFile(file: FileInfo) {

    }
}