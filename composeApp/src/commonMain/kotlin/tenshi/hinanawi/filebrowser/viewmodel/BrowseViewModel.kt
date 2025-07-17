package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.model.response.FileType
import tenshi.hinanawi.filebrowser.model.response.toAddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.util.BreadCrumbNavigator
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
    val favoriteFilesMap: Map<String, Long> = emptyMap(),
    val favorites: List<FavoriteDto> = emptyList(),
    val fileLoading: Boolean = false,
    val previewItem: FileInfo? = null
  )

  sealed class Event {
    object AddFileToFavoriteSuccess : Event()
    object CancelFavoriteFileSuccess : Event()
    object NoImagePreview : Event()
    object IsLastImage : Event()
    object IsFirstImage : Event()
    object TryingPreviewNull : Event()
  }

  // kotlin编译器的类型检查有bug啊，还说进入了死循环，需要手动声明类型
  // 编译构建倒是不会出问题，但是IDE划红线太烦了
  val navigator: BreadCrumbNavigator = BreadCrumbNavigator(onPathChanged = ::refreshFiles)

  private val _event = MutableSharedFlow<Event>()
  val event = _event.asSharedFlow()

  private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
  private val _favoriteFilesMap = MutableStateFlow<Map<String, Long>>(emptyMap())
  private val _favorites = MutableStateFlow<List<FavoriteDto>>(emptyList())

  private val _previewItem = MutableStateFlow<FileInfo?>(null)
  private val _fileLoading = MutableStateFlow(false)

  val uiState = combine(
    _files,
    _favoriteFilesMap,
    _favorites,
    _fileLoading,
    _previewItem,
  ) { files, favoriteExistSet, favorites, loading, preview ->
    BrowserUiState(
      files = files,
      favoriteFilesMap = favoriteExistSet,
      favorites = favorites,
      fileLoading = loading,
      previewItem = preview
    )
  }
    .distinctUntilChanged()
    .catch { e ->
      ErrorHandler.handleException(e)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = BrowserUiState()
    )

  private val _currentFavoriteFile = MutableStateFlow<FileInfo?>(null)

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

  init {
    getData()
  }

  fun refreshFiles() = viewModelScope.launch {
    _fileLoading.value = true
    try {
      _files.value = filesRepository.getFiles(navigator.requestPath)
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
      _files.value = emptyList()
    } finally {
      _fileLoading.value = false
    }
  }

  private fun refreshFavoriteFiles() = viewModelScope.launch {
    try {
      val favoriteFiles = favoriteRepository.getAllFavoriteFiles()
      _favoriteFilesMap.value = favoriteFiles.associate { it.filePath to it.id }
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
    }
  }

  private fun refreshFavorites() = viewModelScope.launch {
    try {
      _favorites.value = favoriteRepository.getFavorites()
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
    }
  }

  private fun getData() {
    refreshFiles()
    refreshFavorites()
    refreshFavoriteFiles()
  }

  fun setCurrentFavoriteFile(file: FileInfo?) {
    _currentFavoriteFile.value = file
  }

  fun addFileToFavorite(favoriteId: Long) = viewModelScope.launch {
    val file = _currentFavoriteFile.value ?: return@launch
    val result = favoriteRepository.addFileToFavorite(file.toAddFileToFavoriteRequest(), favoriteId)
    if (result) {
      refreshFavoriteFiles()
      _event.emit(Event.AddFileToFavoriteSuccess)
      EventBus.emit(EventBus.Event.NotifyFavoriteFileAdd)
    }
  }

  fun cancelFavoriteFile(favoriteFileId: Long) = viewModelScope.launch {
    val result = favoriteRepository.deleteFavoriteFile(favoriteFileId)
    if (result) {
      refreshFavoriteFiles()
      _event.emit(Event.CancelFavoriteFileSuccess)
      EventBus.emit(EventBus.Event.NotifyFavoriteFileRemove)
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


  fun openPreview(previewItem: FileInfo?) = viewModelScope.launch {
    if (previewItem == null) {
      _event.emit(Event.TryingPreviewNull)
      return@launch
    }
    _previewItem.value = previewItem
  }

  fun closePreview() {
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
      openPreview(nextImage)
    } else {
      _event.emit(Event.IsLastImage)
      openPreview(_firstImage)
    }
  }

  fun previousImagePreview() = viewModelScope.launch {
    if (_currentImageIndex == -1) {
      _event.emit(Event.NoImagePreview)
      return@launch
    }
    val previousImage = uiState.value.files.firstBefore(_currentImageIndex) { it.type == FileType.Image }
    if (previousImage != null) {
      openPreview(previousImage)
    } else {
      _event.emit(Event.IsFirstImage)
      openPreview(_lastImage)
    }
  }
}