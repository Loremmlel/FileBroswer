package tenshi.hinanawi.filebrowser.viewmodel

import androidx.lifecycle.ViewModel
import tenshi.hinanawi.filebrowser.data.repo.FilesRepository
import kotlin.reflect.KClass

object ViewModelFactory {
    fun <T: ViewModel> create(
        modelClass: KClass<T>,
        vararg dependencies: Any
    ): T = when (modelClass) {
        BrowseViewModel::class -> {
            val repository = dependencies.first() as FilesRepository
            BrowseViewModel(repository) as T
        }
        FavoriteViewModel::class -> {
            FavoriteViewModel() as T
        }
        else -> throw IllegalArgumentException("未知ViewModel类型")
    }
}