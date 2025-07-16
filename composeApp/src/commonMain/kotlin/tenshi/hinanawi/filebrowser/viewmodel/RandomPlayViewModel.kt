package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.RandomRepository
import tenshi.hinanawi.filebrowser.model.response.FileInfo
import tenshi.hinanawi.filebrowser.util.ErrorHandler
import tenshi.hinanawi.filebrowser.util.currentTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class RandomPlayViewModel(
  private val randomRepository: RandomRepository
) : ViewModel() {
  data class RandomPlayUiState(
    val loading: Boolean = false,
    val videoFiles: List<FileInfo> = emptyList()
  )

  private val _requestPath = MutableSharedFlow<String>()
  private val _clearTrigger = MutableSharedFlow<Unit>()

  private val _requestTimeMap: MutableMap<String, Long> = mutableMapOf()
  private val _cacheMap: MutableMap<String, List<FileInfo>> = mutableMapOf()

  private val cacheDuration: Long = 60 * 60 * 1000

  private val _videoFilesFlow = merge(
    _requestPath
      .flatMapLatest { path ->
        val requestTime = currentTimeMillis()
        val lastRequestTime = _requestTimeMap[path] ?: 0
        val cacheVideoFiles = _cacheMap[path]
        if (requestTime - lastRequestTime <= cacheDuration && cacheVideoFiles != null) {
          flowOf(cacheVideoFiles)
            .onCompletion {
              _loading.value = false
            }
        } else {
          randomRepository.getAllVideo(path)
            .catch { e ->
              ErrorHandler.handleException(e)
              emit(emptyList())
              _loading.value = false
            }
            .onEach { videoFiles ->
              _requestTimeMap[path] = requestTime
              _cacheMap[path] = videoFiles
            }
            .onCompletion {
              _loading.value = false
            }
        }
      },
    _clearTrigger.map { emptyList() }
  )
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = emptyList()
    )

  private val _loading = MutableStateFlow(false)

  val uiState = combine(
    _videoFilesFlow,
    _loading
  ) { videoFiles, loading ->
    RandomPlayUiState(
      loading = loading,
      videoFiles = videoFiles
    )
  }
    .catch { e ->
      ErrorHandler.handleException(e)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = RandomPlayUiState()
    )

  fun getAllVideos(path: String) = viewModelScope.launch {
    _loading.value = true
    _requestPath.emit(path)
  }

  fun getRandomVideo(): FileInfo? {
    val videoFiles = uiState.value.videoFiles
    return videoFiles.randomOrNull()
  }

  fun pathChange() = viewModelScope.launch {
    _clearTrigger.emit(Unit)
  }
}