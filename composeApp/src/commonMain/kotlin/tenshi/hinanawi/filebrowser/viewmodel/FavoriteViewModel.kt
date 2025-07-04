package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.util.ErrorHandler

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  val navigator: BreadCrumbNavigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Loading)
  val uiState = _uiState.asStateFlow()

  private val _currentFavoriteId = MutableStateFlow(navigator.currentId)

  init {
    viewModelScope.launch {
      _currentFavoriteId.flatMapLatest { favoriteId ->
        favoriteRepository.getFavorite(favoriteId?.toLongOrNull())
          .onStart {
            _uiState.value = FavoriteUiState.Loading
          }
          .catch { exception ->
            _uiState.value = FavoriteUiState.Success()
            ErrorHandler.handleException(exception)
          }
      }
        .collect { favorite ->
          _uiState.value = FavoriteUiState.Success(
            currentFavorite = favorite
          )
        }
    }
  }

  fun getData() {
    _currentFavoriteId.value = navigator.currentId
  }

  suspend fun createFavorite(parentId: Long? = null, name: String, sortOrder: Int = 0) {
    viewModelScope.launch {

    }
  }
}

sealed class FavoriteUiState {
  object Loading : FavoriteUiState()
  data class Success(
    val tree: List<FavoriteDto> = emptyList(),
    val currentFavorite: FavoriteDto? = null
  ) : FavoriteUiState()
}