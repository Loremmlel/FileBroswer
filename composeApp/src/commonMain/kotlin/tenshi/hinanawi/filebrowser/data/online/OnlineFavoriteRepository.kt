package tenshi.hinanawi.filebrowser.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto

class OnlineFavoriteRepository: FavoriteRepository, BaseOnlineRepository() {
  override fun getFavoriteTree(parentId: Long?): Flow<List<FavoriteDto>> = flow {

  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto {
    TODO("Not yet implemented")
  }

  override fun getFavorite(id: Long?): Flow<FavoriteDto> = flow {

  }
}