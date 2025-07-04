package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.model.*
import tenshi.hinanawi.filebrowser.plugin.safeExecute
import tenshi.hinanawi.filebrowser.schema.FavoriteService
import tenshi.hinanawi.filebrowser.util.contentTypeJson

fun Route.favorite() {
  val favoriteService = FavoriteService()

  route("/favorites") {
    // 获取收藏夹树
    get {
      call.safeExecute {
        contentTypeJson()
        val parentId = call.queryParameters["parentId"]?.toLongOrNull()
        val favorites = favoriteService.getFavoriteTree(parentId)
        respond(
          HttpStatusCode.OK,
          Response(200, Message.Success, favorites)
        )
      }
    }

    // 创建收藏夹
    post {
      call.safeExecute {
        contentTypeJson()
        val request = receive<CreateFavoriteRequest>()
        val favorite = favoriteService.createFavorite(
          parentId = request.parentId,
          name = request.name,
          sortOrder = request.sortOrder
        )
        respond(
          HttpStatusCode.Created,
          Response(201, Message.Success, favorite)
        )
      }
    }
  }

  // 获取单个收藏夹详情
  get("/{id}") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull()

      val favorite = favoriteService.getFavoriteDetail(favoriteId)
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, favorite)
      )
    }
  }

  // 更新收藏夹
  put("/{id}") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteIdUndefined, null)
        )
        return@safeExecute
      }

      val request = receive<UpdateFavoriteRequest>()
      val favorite = favoriteService.updateFavorite(
        favoriteId = favoriteId,
        name = request.name,
        sortOrder = request.sortOrder
      )
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, favorite)
      )
    }
  }

  // 删除收藏夹
  delete("/{id}") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteIdUndefined, null)
        )
        return@safeExecute
      }

      val success = favoriteService.deleteFavorite(favoriteId)
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, success)
      )
    }
  }

  // 移动收藏夹
  patch("/{id}/move") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteIdUndefined, null)
        )
        return@safeExecute
      }

      val request = receive<MoveFavoriteRequest>()
      val favorite = favoriteService.moveFavorite(favoriteId, request.newParentId)
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, favorite)
      )
    }
  }

  // 添加文件到收藏夹
  post("/{id}/files") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteIdUndefined, null)
        )
        return@safeExecute
      }

      val request = receive<AddFileToFavoriteRequest>()
      val favoriteFileDto = FavoriteFileDto(
        id = 0, // 临时ID，会在创建时分配
        favoriteId = favoriteId,
        filename = request.filename,
        fileSize = request.fileSize,
        fileType = request.fileType,
        filePath = request.filePath,
        lastModified = request.lastModified,
        isDirectory = request.isDirectory,
        createdAt = System.currentTimeMillis()
      )
      val favoriteFile = favoriteService.addFileToFavorite(favoriteId, favoriteFileDto)
      respond(
        HttpStatusCode.Created,
        Response(201, Message.Success, favoriteFile)
      )
    }
  }

  // 批量添加文件到收藏夹
  post("/{id}/files/batch") {
    call.safeExecute {
      contentTypeJson()
      val favoriteId = call.parameters["id"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteIdUndefined, null)
        )
        return@safeExecute
      }

      val request = receive<AddFilesToFavoriteRequest>()
      val favoriteFileDtos = request.files.map { file ->
        FavoriteFileDto(
          id = 0, // 临时ID，会在创建时分配
          favoriteId = favoriteId,
          filename = file.filename,
          fileSize = file.fileSize,
          fileType = file.fileType,
          filePath = file.filePath,
          lastModified = file.lastModified,
          isDirectory = file.isDirectory,
          createdAt = System.currentTimeMillis()
        )
      }
      val favoriteFiles = favoriteService.addFilesToFavorite(favoriteId, favoriteFileDtos)
      respond(
        HttpStatusCode.Created,
        Response(201, Message.Success, favoriteFiles)
      )

    }
  }

  // 删除收藏文件
  delete("/files/{fileId}") {
    call.safeExecute {
      contentTypeJson()
      val favoriteFileId = call.parameters["fileId"]?.toLongOrNull() ?: run {
        respond(
          HttpStatusCode.BadRequest,
          Response<Unit>(400, Message.FavoriteFileIdUndefined, null)
        )
        return@safeExecute
      }
      val success = favoriteService.removeFavoriteFile(favoriteFileId)
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, success)
      )
    }
  }

  // 批量删除收藏文件
  delete("/files") {
    call.safeExecute {
      contentTypeJson()
      val request = receive<RemoveFavoriteFilesRequest>()
      val deleteCount = favoriteService.removeFavoriteFiles(request.favoriteFileIds)
      respond(
        HttpStatusCode.OK,
        Response(200, Message.Success, mapOf("deletedCount" to deleteCount))
      )
    }
  }
}