package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.FavoriteDto
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineFavoriteRepository: FavoriteRepository, BaseOnlineRepository() {
  override fun getFavoriteTree(parentId: Long?): Flow<List<FavoriteDto>> = flow {

  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto? = try {
    client.post("/favorites") {
      setBody(request)
    }.body<Response<FavoriteDto>>().data ?: throw Exception("Failed to create favorite")
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    null
  }

  override fun getFavorite(id: Long?): Flow<FavoriteDto> = flow {
    val favorite = client.get("/favorites/$id").body<Response<FavoriteDto>>()
    favorite.data?.let { emit(it) }
  }
}