package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository

class BrowseViewModel(
    private val filesRepository: FilesRepository
): ViewModel() {
    var currentPath: String = "/"
}