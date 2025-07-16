package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.data.repo.FavoriteRepository
import tenshi.hinanawi.filebrowser.model.request.AddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.model.request.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto
import tenshi.hinanawi.filebrowser.util.ErrorHandler

class OnlineFavoriteRepository : FavoriteRepository, BaseOnlineRepository() {
  override fun getFavorites(): Flow<List<FavoriteDto>> = flow {
    val data = client.get("/favorites").body<Response<List<FavoriteDto>>>().data
    // flow里一定要记得emit
    emit(data ?: emptyList())
  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto? = try {
    client.post("/favorites") {
      setBody(request)
      contentType(ContentType.Application.Json)
    }.body<Response<FavoriteDto>>().data
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    null
  }

  override fun getFavoriteDetail(id: Long): Flow<FavoriteDto?> = flow {
    val response = client.get("/favorites/$id").body<Response<FavoriteDto>>()
    emit(response.data)
  }

  override suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean = try {
    client.post("/favorites/${favoriteId}/files") {
      setBody(request)
      contentType(ContentType.Application.Json)
    }.body<Response<FavoriteFileDto>>().data != null
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }

  override fun getAllFavoriteFiles(): Flow<List<FavoriteFileDto>> = flow {
    val data = client.get("/favorites/files").body<Response<List<FavoriteFileDto>>>().data
    emit(data ?: emptyList())
  }

  override suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean = try {
    client.delete("/favorites/files/${favoriteFileId}").body<Response<Boolean>>().data ?: false
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }
}