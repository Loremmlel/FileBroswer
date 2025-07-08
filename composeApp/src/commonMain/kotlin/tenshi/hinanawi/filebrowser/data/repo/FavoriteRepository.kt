package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto

interface FavoriteRepository {
  fun getFavoriteTree(parentId: Long? = null): Flow<List<FavoriteDto>>

  suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto?

  fun getFavorite(id: Long? = null): Flow<FavoriteDto>
}