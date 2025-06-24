package tenshi.hinanawi.filebrowser.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import tenshi.hinanawi.filebrowser.util.getFileType
import java.nio.file.Paths

fun Application.files() {
    routing {
        get("/files") {
            try {
                val path = call.queryParameters["path"]
                if (path == null || !path.startsWith('/')) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<FileInfo>(400, Message.FilesNotFound, null)
                    )
                    return@get
                }
                val normalizedPath = Paths.get(AppConfig.BASE_DIR, path).normalize()
                if (!normalizedPath.startsWith(AppConfig.BASE_DIR)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        Response<FileInfo>(403, Message.FilesForbidden, null)
                    )
                    return@get
                }
                val dir = normalizedPath.toFile()
                if (!dir.isDirectory) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response<FileInfo>(400, Message.FilesIsNotDirectory, null)
                    )
                    return@get
                }
                val res = mutableListOf<FileInfo>()
                dir.listFiles().forEach {
                    if (it.isHidden) {
                        return@forEach
                    }
                    res.add(FileInfo(
                        it.name,
                        it.length().toString(),
                        it.isDirectory,
                        it.getFileType(),
                        it.lastModified(),
                        it.path.substring(AppConfig.BASE_DIR.length)
                    ))
                }
                res.sortWith(Comparator { a, b ->
                    if (a.isDirectory && !b.isDirectory) {
                        -1
                    } else if (!a.isDirectory && b.isDirectory) {
                        1
                    } else {
                        a.name.compareTo(b.name)
                    }
                })
                call.respond(
                    HttpStatusCode.OK,
                    Response<List<FileInfo>>(200, Message.Success, res)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response<FileInfo>(500, Message.InternalServerError, null)
                )
                e.printStackTrace()
            }
        }
        delete("/files") {
            try {
                val path = call.queryParameters["path"]
                if (path == null || !path.startsWith('/')) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(400, Message.FilesNotFound, null)
                    )
                    return@delete
                }
                val normalizedPath = Paths.get(AppConfig.BASE_DIR, path).normalize()
                if (!normalizedPath.startsWith(AppConfig.BASE_DIR)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        Response(403, Message.FilesForbidden, null)
                    )
                    return@delete
                }
                val file = normalizedPath.toFile()
                if (!file.exists()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(400, Message.FilesNotFound, null)
                    )
                    return@delete
                }
                if (!file.delete()) {
                    if (file.isDirectory) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            Response(400, Message.FilesDirectoryMustEmptyWhileDelete, null)
                        )
                        return@delete
                    }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Response(500, Message.Failed, null)
                    )
                    log.warn("文件${file.path}删除失败")
                    return@delete
                }
                call.respond(
                    HttpStatusCode.NoContent,
                    Response(204, Message.Success, null)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response(500, Message.InternalServerError, null)
                )
                e.printStackTrace()
            }
        }
    }
}