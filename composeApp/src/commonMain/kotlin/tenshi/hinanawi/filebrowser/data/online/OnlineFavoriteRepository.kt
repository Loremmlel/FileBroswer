package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineFavoriteRepository : FavoriteRepository, BaseOnlineRepository() {
  override fun getFavorites(): Flow<List<FavoriteDto>> = flow<List<FavoriteDto>> {
    client.get("/favorites").body<Response<List<FavoriteDto>>>().data ?: emptyList<FavoriteDto>()
  }.catch { e ->
    ErrorHandler.handleException(e)
    emit(emptyList())
  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto? = try {
    client.post("/favorites") {
      setBody(request)
    }.body<Response<FavoriteDto>>().data
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    null
  }

  override fun getFavoriteDetail(id: Long): Flow<FavoriteDto?> = flow<FavoriteDto?> {
    val favorite = client.get("/favorites/$id").body<Response<FavoriteDto>>()
    favorite.data?.let { emit(it) }
  }.catch { e ->
    ErrorHandler.handleException(e)
    emit(null)
  }
}