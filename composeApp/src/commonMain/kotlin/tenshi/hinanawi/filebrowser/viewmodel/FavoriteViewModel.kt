package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator

class FavoriteViewModel(
  private val favoriteRepository: FavoriteRepository
) : ViewModel() {
  val navigator: BreadCrumbNavigator = BreadCrumbNavigator(onPathChanged = ::getData)

  fun getData() {

  }
}