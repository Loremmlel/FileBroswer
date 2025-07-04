package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.FavoriteDto

class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  val navigator: BreadCrumbNavigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Loading)
  val uiState = _uiState.asStateFlow()

  private val _currentFavoriteId = MutableStateFlow(navigator.currentId)

  init {

  }

  fun getData() {
    _currentFavoriteId.value = navigator.currentId
  }
}

sealed class FavoriteUiState {
  object Loading : FavoriteUiState()
  data class FavoriteTree(
    val tree: List<FavoriteDto>,
    val currentFavorite: FavoriteDto
  ) : FavoriteUiState()
}