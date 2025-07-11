package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.model.AddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.model.FavoriteFileDto

interface FavoriteRepository {
  fun getFavorites(): Flow<List<FavoriteDto>>

  suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto?

  fun getFavoriteDetail(id: Long): Flow<FavoriteDto?>

  suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean

  fun getAllFavoriteFiles(): Flow<List<FavoriteFileDto>>

  suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean
}