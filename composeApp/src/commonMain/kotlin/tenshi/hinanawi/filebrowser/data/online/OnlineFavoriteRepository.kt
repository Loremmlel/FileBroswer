package tenshi.hinanawi.filebrowser.data.online

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto

class OnlineFavoriteRepository: FavoriteRepository, BaseOnlineRepository() {
  override fun getFavoriteTree(parentId: Long?): Flow<List<FavoriteDto>> {
    TODO("Not yet implemented")
  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto {
    TODO("Not yet implemented")
  }

  override fun getFavorite(id: Long?): Flow<FavoriteDto> {
    TODO("Not yet implemented")
  }
}