package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.util.ErrorHandler

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  data class FavoriteUiState(
    val favorites: List<FavoriteDto> = emptyList(),
    val currentFavorite: FavoriteDto? = null,
    val loading: Boolean = true
  )

  sealed class Event {
    object CreateSuccess : Event()
  }

  private val _event = MutableSharedFlow<Event>()
  val event = _event.asSharedFlow()

  private val _refreshTrigger = MutableSharedFlow<Unit>()
  private val _favoritesFlow = _refreshTrigger
    .onStart { emit(Unit) }
    .flatMapLatest {
      favoriteRepository
        .getFavorites()
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

  private val _currentFavoriteId = MutableStateFlow<Long?>(null)
  private val _currentFavoriteFlow = _currentFavoriteId
    .filterNotNull()
    .flatMapLatest {
      favoriteRepository
        .getFavoriteDetail(it)
        .catch { e ->
          ErrorHandler.handleException(e)
          emit(null)
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

  val uiState = combine(
    _favoritesFlow,
    _currentFavoriteFlow,
    _currentFavoriteId
  ) { favorites, currentFavorite, currentFavoriteId ->
    FavoriteUiState(
      favorites = favorites,
      currentFavorite = currentFavorite,
      loading = currentFavoriteId != null && currentFavorite == null
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

  suspend fun refreshFavorites() {
    _refreshTrigger.emit(Unit)
  }

  fun createFavorite(name: String, sortOrder: Int = 0) = viewModelScope.launch {
    val newFavorite = favoriteRepository.createFavorite(CreateFavoriteRequest(name, sortOrder))
    if (newFavorite != null) {
      _event.emit(Event.CreateSuccess)
      delay(1000)
      refreshFavorites()
    }
  }
}
