package tenshi.hinanawi.filebrowser.plugins

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.Response
import java.io.File

fun Application.files() {
    routing {
        get("/files") {
            try {
                val path = call.pathParameters["path"]
                if (path == null || !path.startsWith('/')) {
                    call.respond(Response<FileInfo>(400, "查询参数不正确", null))
                    return@get
                }
                val dir = File(AppConfig.BASE_DIR + path)
                if (!dir.absolutePath.startsWith(AppConfig.BASE_DIR)) {
                    call.respond(Response<FileInfo>(403, "无权访问", null))
                    return@get
                }
                if (!dir.isDirectory) {
                    call.respond(Response<FileInfo>(400, "查询参数不正确", null))
                    return@get
                }
                val res = mutableListOf<FileInfo>()
                dir.listFiles().forEach {
                    res.add(FileInfo(it.name, it.length().toString(), it.isDirectory))
                }
                call.respond(Response<List<FileInfo>>(200, "成功", res))
            } catch (e: Exception) {
                call.respond(Response<FileInfo>(500, e.message, null))
            }
        }
    }
}