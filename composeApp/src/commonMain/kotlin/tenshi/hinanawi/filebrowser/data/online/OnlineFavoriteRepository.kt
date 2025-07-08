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
  override fun getFavorites(): Flow<List<FavoriteDto>> = flow {
    val data = client.get("/favorites").body<Response<List<FavoriteDto>>>().data
    // flow里一定要记得emit
    emit(data ?: emptyList())
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

  override fun getFavoriteDetail(id: Long): Flow<FavoriteDto?> = flow {
    val response = client.get("/favorites/$id").body<Response<FavoriteDto>>()
    emit(response.data)
  }.catch { e ->
    ErrorHandler.handleException(e)
    emit(null)
  }
}