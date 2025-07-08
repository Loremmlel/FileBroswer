package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto

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
    object CreateSuccess: Event()
  }

  private val _uiState = MutableStateFlow(FavoriteUiState())
  val uiState = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<Event>()
  val events = _events.asSharedFlow()

  init {
    loadFavorites()
  }

  fun createFavorite(name: String, sortOrder: Int = 0) = viewModelScope.launch {
    val newFavorite = favoriteRepository.createFavorite(
      CreateFavoriteRequest(
        name = name,
        sortOrder = sortOrder
      )
    )
    if (newFavorite != null) {
      _events.tryEmit(Event.CreateSuccess)
    }
  }

  fun loadFavoriteDetail(id: Long) = viewModelScope.launch {
    favoriteRepository.getFavoriteDetail(id)
      .onStart {
        _uiState.update {
          it.copy(loading = true)
        }
      }
      .collect { detail ->
        _uiState.update {
          it.copy(
            currentFavorite = detail,
            loading = false
          )
        }
      }
  }

  private fun loadFavorites() = viewModelScope.launch {
    favoriteRepository.getFavorites()
      .onStart {
        _uiState.update {
          it.copy(loading = true)
        }
      }
      .collect { favorites ->
        _uiState.update {
          it.copy(
            favorites = favorites,
            loading = false
          )
        }
      }
  }
}
