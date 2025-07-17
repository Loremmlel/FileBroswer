package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.request.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.util.ErrorHandler

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  data class FavoriteUiState(
    val favorites: List<FavoriteDto> = emptyList(),
    val loading: Boolean = true
  )

  sealed class Event {
    object CreateSuccess : Event()
  }

  private val _event = MutableSharedFlow<Event>()
  val event = _event.asSharedFlow()

  private val _favorites = MutableStateFlow<List<FavoriteDto>>(emptyList())
  private val _loading = MutableStateFlow(false)

  val uiState = combine(
    _favorites,
    _loading
  ) { favorites, loading ->
    FavoriteUiState(
      favorites = favorites,
      loading = loading
    )
  }
    .catch { e ->
      ErrorHandler.handleException(e)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = FavoriteUiState()
    )

  init {
    getData()
    viewModelScope.launch {
      EventBus.event.collect {
        when (it) {
          is EventBus.Event.NotifyFavoriteFileAdd,
          EventBus.Event.NotifyFavoriteFileRemove -> refreshFavorites()
        }
      }
    }
  }

  private fun getData() {
    refreshFavorites()
  }

  fun refreshFavorites() = viewModelScope.launch {
    _loading.value = true
    try {
      _favorites.value = favoriteRepository.getFavorites()
    } catch (e: Exception) {
      ErrorHandler.handleException(e)
    } finally {
      _loading.value = false
    }
  }

  fun createFavorite(name: String, sortOrder: Int = 0) = viewModelScope.launch {
    val newFavorite = favoriteRepository.createFavorite(CreateFavoriteRequest(name, sortOrder))
    if (newFavorite != null) {
      _event.emit(Event.CreateSuccess)
      delay(500)
      refreshFavorites()
    }
  }
}
