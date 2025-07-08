package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.util.ErrorHandler

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  val navigator: BreadCrumbNavigator = BreadCrumbNavigator(onPathChanged = ::getData)

  private val _uiState = MutableStateFlow(FavoriteUiState())
  val uiState = _uiState.asStateFlow()

  private val _currentFavoriteId = MutableStateFlow(navigator.currentId)

  init {
    viewModelScope.launch {
      _currentFavoriteId.flatMapLatest { favoriteId ->
        favoriteRepository.getFavorite(favoriteId?.toLongOrNull())
          .onStart {
            _uiState.update {
              it.copy(
                loading = true
              )
            }
          }
          .catch { exception ->
            _uiState.update {
              it.copy(
                loading = false,
                currentFavorite = null
              )
            }
            ErrorHandler.handleException(exception)
          }
      }
        .collect { favorite ->
          _uiState.update {
            it.copy(
              loading = false,
              currentFavorite = favorite
            )
          }
        }
    }
  }

  fun getData() {
    _currentFavoriteId.value = navigator.currentId
  }

  fun createFavorite(name: String, sortOrder: Int = 0) = viewModelScope.launch {
    val newFavorite = favoriteRepository.createFavorite(
      CreateFavoriteRequest(
        parentId = _currentFavoriteId.value?.toLongOrNull(),
        name = name,
        sortOrder = sortOrder
      )
    ) ?: return@launch
    _uiState.update {
      it.copy(
        currentFavorite = it.currentFavorite?.copy(
          children = it.currentFavorite.children.plus(newFavorite)
        )
      )
    }
    Toast.makeText("收藏夹创建成功", Toast.LENGTH_LONG).show()
  }
}


data class FavoriteUiState(
  val tree: List<FavoriteDto> = emptyList(),
  val currentFavorite: FavoriteDto? = null,
  val loading: Boolean = true
)
