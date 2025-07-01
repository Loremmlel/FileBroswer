package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.RandomRepository
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.util.ErrorHandler
import tenshi.hinanawi.filebrowser.util.currentTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class RandomPlayViewModel(
  private val randomRepository: RandomRepository
) : ViewModel() {
  private val _uiState = MutableStateFlow(UiState())
  val uiState = _uiState.asStateFlow()

  private val _requestPath = MutableSharedFlow<String>()

  private val _requestTimeMap: MutableMap<String, Long> = mutableMapOf()
  private val _cacheMap: MutableMap<String, List<FileInfo>> = mutableMapOf()

  private val cacheDuration: Long = 60 * 60 * 1000

  init {
    viewModelScope.launch {
      _requestPath
        .flatMapLatest<String, List<FileInfo>> { path ->
          val currentTime = currentTimeMillis()
          val lastRequestTime = _requestTimeMap[path] ?: 0
          if (currentTime - lastRequestTime < cacheDuration && _cacheMap.containsKey(path)) {
            flowOf(_cacheMap[path]!!)
              .onStart {
                _uiState.update {
                  it.copy(loading = true)
                }
              }
          } else {
            randomRepository.getAllVideo(path)
              .onStart {
                _uiState.update {
                  it.copy(loading = true)
                }
              }
              .catch { exception ->
                ErrorHandler.handleException(exception)
                _uiState.update {
                  it.copy(loading = false)
                }
              }
              .onEach { videoFiles ->
                _requestTimeMap[path] = currentTime
                _cacheMap[path] = videoFiles
              }
          }
        }
        .collect { videoFiles ->
          _uiState.update {
            it.copy(
              loading = false,
              videoFiles = videoFiles
            )
          }
        }
    }
  }

  fun getAllVideos(path: String) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          videoFiles = emptyList()
        )
      }
      _requestPath.emit(path)
    }
  }

  fun getRandomVideo(): FileInfo? {
    val videoFiles = uiState.value.videoFiles
    if (videoFiles.isNotEmpty()) {
      val randomIndex = (0 until videoFiles.size).random()
      val randomVideo = videoFiles[randomIndex]
      return randomVideo
    }
    return null
  }
}

data class UiState(
  val loading: Boolean = false,
  val videoFiles: List<FileInfo> = emptyList()
)