package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
  data class BrowserUiState(
    val files: List<FileInfo> = emptyList(),
    val fileLoading: Boolean = false,
    val previewItem: FileInfo? = null,
    val playingVideo: FileInfo? = null
  )

  sealed class Event {
    object AddFavoriteSuccess : Event()
    object NoImagePreview : Event()
    object IsLastImage : Event()
    object IsFirstImage : Event()
    object TryingPreviewNull: Event()
  }

  val navigator = BreadCrumbNavigator(onPathChanged = ::refreshFiles)

  private val _event = MutableSharedFlow<Event>()
  val event = _event.asSharedFlow()

  private val _refreshTrigger = MutableSharedFlow<Unit>()
  private val _currentPath = MutableStateFlow(navigator.requestPath)

  private val _filesFlow = combine(_refreshTrigger.onStart { emit(Unit) }, _currentPath) { _, path ->
    path
  }
    .flatMapLatest { path ->
      filesRepository.getFiles(path)
        .catch { e ->
          ErrorHandler.handleException(e)
          emit(emptyList())
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  private val _previewItem = MutableStateFlow<FileInfo?>(null)
  private val _fileLoading = MutableStateFlow(false)

  val uiState = combine(
    _filesFlow,
    _fileLoading,
    _previewItem
  ) { files, loading, preview ->
    BrowserUiState(
      files = files,
      fileLoading = loading,
      previewItem = preview
    )
  }
    .catch { e ->
      ErrorHandler.handleException(e)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = BrowserUiState()
    )

  private val _firstImage
    get() = uiState.value.files.firstOrNull {
      it.type == FileType.Image
    }

  private val _lastImage
    get() = uiState.value.files.lastOrNull {
      it.type == FileType.Image
    }

  private val _currentImageIndex
    get() = uiState.value.files.indexOfFirst {
      it.name == uiState.value.previewItem?.name
    }

  suspend fun refreshFiles() {
    closeImagePreview()
    _currentPath.value = navigator.requestPath
    _refreshTrigger.emit(Unit)
  }

  fun addFavorite(file: FileInfo, favoriteId: Long) = viewModelScope.launch {
    val result = favoriteRepository.addFileToFavorite(file.toAddFileToFavoriteRequest(), favoriteId)
    if (result) {
      _event.emit(Event.AddFavoriteSuccess)
    }
  }

  fun deleteFile(file: FileInfo) = viewModelScope.launch {
    filesRepository.deleteFile("${navigator.requestPath}/${file.name}")
    refreshFiles()
  }


  fun downloadFile(file: FileInfo) = viewModelScope.launch {
    val filePath = "${navigator.requestPath}/${file.name}"
    filesRepository.downloadFile(filePath, file.name)
  }


  fun openImagePreview(image: FileInfo?) = viewModelScope.launch {
    if (image == null) {
      _event.emit(Event.TryingPreviewNull)
      return@launch
    }
    _previewItem.value = image
  }

  fun closeImagePreview() {
    _previewItem.value = null
  }

  fun nextImagePreview() = viewModelScope.launch {
    if (_currentImageIndex == -1) {
      _event.emit(Event.NoImagePreview)
      return@launch
    }
    // 写反了，耻辱
    val nextImage = uiState.value.files.firstAfter(_currentImageIndex) { it.type == FileType.Image }
    if (nextImage != null) {
      openImagePreview(nextImage)
    } else {
      _event.emit(Event.IsLastImage)
      openImagePreview(_firstImage)
    }
  }

  fun previousImagePreview() = viewModelScope.launch {
    if (_currentImageIndex == -1) {
      _event.emit(Event.NoImagePreview)
      return@launch
    }
    val previousImage = uiState.value.files.firstBefore(_currentImageIndex) { it.type == FileType.Image }
    if (previousImage != null) {
      openImagePreview(previousImage)
    } else {
      _event.emit(Event.IsFirstImage)
      openImagePreview(_lastImage)
    }
  }

  fun playVideo(video: FileInfo) {

  }
}