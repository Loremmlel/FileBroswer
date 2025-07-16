package tenshi.hinanawi.filebrowser.data.repo

import kotlinx.coroutines.flow.Flow
import tenshi.hinanawi.filebrowser.model.request.AddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.model.request.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto

interface FavoriteRepository {
  fun getFavorites(): Flow<List<FavoriteDto>>

  suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto?

  fun getFavoriteDetail(id: Long): Flow<FavoriteDto?>

  suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean

  fun getAllFavoriteFiles(): Flow<List<FavoriteFileDto>>

  suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean
}