package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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

    fun getData() {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            filesRepository.getFiles(navigator.requestPath)
                .catch {
                    ErrorHandler.handleException(it)
                }
                .onEach {
                    _files.value = it
                }
                .collect()
        }
    }
}