package tenshi.hinanawi.filebrowser.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
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
                    res.add(FileInfo(it.name, it.length().toString(), it.isDirectory))
                }
                call.respond(
                    HttpStatusCode.OK,
                    Response<List<FileInfo>>(200, Message.Success, res)
                )
            } catch (e: Exception) {
                call.respond(Response<FileInfo>(500, Message.InternalServerError, null))
                e.printStackTrace()
            }
        }
    }
}