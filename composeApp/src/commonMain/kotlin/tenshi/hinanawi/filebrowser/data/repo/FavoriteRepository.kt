package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.request.AddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.model.request.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto
import tenshi.hinanawi.filebrowser.util.ErrorHandler

interface FavoriteRepository {
  fun getFavorites(): Flow<List<FavoriteDto>>

  suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto?

  fun getFavoriteDetail(id: Long): Flow<FavoriteDto?>

  suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean

  fun getAllFavoriteFiles(): Flow<List<FavoriteFileDto>>

  suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean
}

class OnlineFavoriteRepository : FavoriteRepository, BaseOnlineRepository() {
  private val basePath = "/favorites"

  override fun getFavorites(): Flow<List<FavoriteDto>> = flow {
    val data = client.get(basePath).body<Response<List<FavoriteDto>>>().data
    // flow里一定要记得emit
    emit(data ?: emptyList())
  }

  override suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto? = try {
    client.post(basePath) {
      setBody(request)
      contentType(ContentType.Application.Json)
    }.body<Response<FavoriteDto>>().data
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    null
  }

  override fun getFavoriteDetail(id: Long): Flow<FavoriteDto?> = flow {
    val response = client.get("$basePath/$id").body<Response<FavoriteDto>>()
    emit(response.data)
  }

  override suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean = try {
    client.post("$basePath/${favoriteId}/files") {
      setBody(request)
      contentType(ContentType.Application.Json)
    }.body<Response<FavoriteFileDto>>().data != null
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }

  override fun getAllFavoriteFiles(): Flow<List<FavoriteFileDto>> = flow {
    val data = client.get("$basePath/files").body<Response<List<FavoriteFileDto>>>().data
    emit(data ?: emptyList())
  }

  override suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean = try {
    client.delete("$basePath/files/${favoriteFileId}").body<Response<Boolean>>().data ?: false
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }
}