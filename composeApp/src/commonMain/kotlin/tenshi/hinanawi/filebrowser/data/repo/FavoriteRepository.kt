package tenshi.hinanawi.filebrowser.data.repo

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.FavoriteFileDto
import tenshi.hinanawi.filebrowser.model.request.AddFileToFavoriteRequest
import tenshi.hinanawi.filebrowser.model.request.CreateFavoriteRequest
import tenshi.hinanawi.filebrowser.util.ErrorHandler

interface FavoriteRepository {
  suspend fun getFavorites(): List<FavoriteDto>

  suspend fun createFavorite(request: CreateFavoriteRequest): FavoriteDto?

  suspend fun getFavoriteDetail(id: Long): FavoriteDto?

  suspend fun addFileToFavorite(request: AddFileToFavoriteRequest, favoriteId: Long): Boolean

  suspend fun getAllFavoriteFiles(): List<FavoriteFileDto>

  suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean
}

class OnlineFavoriteRepository : FavoriteRepository, BaseOnlineRepository() {
  private val basePath = "/favorites"

  override suspend fun getFavorites(): List<FavoriteDto> {
    val data = client.get(basePath).body<Response<List<FavoriteDto>>>().data
    // flow一定要emit ✋😭✋ ✋😭✋ ✋😭✋
    return data ?: emptyList()
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

  override suspend fun getFavoriteDetail(id: Long): FavoriteDto? {
    val response = client.get("$basePath/$id").body<Response<FavoriteDto>>()
    return response.data
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

  override suspend fun getAllFavoriteFiles(): List<FavoriteFileDto> {
    val data = client.get("$basePath/files").body<Response<List<FavoriteFileDto>>>().data
    return data ?: emptyList()
  }

  override suspend fun deleteFavoriteFile(favoriteFileId: Long): Boolean = try {
    client.delete("$basePath/files/${favoriteFileId}").body<Response<Boolean>>().data ?: false
  } catch (e: Exception) {
    ErrorHandler.handleException(e)
    false
  }
}