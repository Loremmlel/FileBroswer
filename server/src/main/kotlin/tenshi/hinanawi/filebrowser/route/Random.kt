package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.*
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.util.contentTypeJson
import tenshi.hinanawi.filebrowser.util.getFileType
import java.io.File

internal fun Application.random() = routing {
    route("/random") {
        install(PathValidator)
        get("/random") {
            try {
                call.contentTypeJson()
                val type = call.queryParameters["type"]?.parseFileType() ?: FileType.Video
                val dir = call.attributes[ValidatedFileKey]
                val files = scanDirectory(dir, type)
                call.respond(
                    HttpStatusCode.OK,
                    Response(200, Message.Success, files)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response(500, Message.InternalServerError, null)
                )
                log.error("接口${call.request.path()}错误: ${e.message}")
            }
        }
    }
}

private fun scanDirectory(dir: File, type: FileType): List<FileInfo> {
    val res = mutableListOf<FileInfo>()
    if (!dir.isDirectory) {
        return res
    }
    for (file in dir.listFiles() ?: emptyArray()) {
        if (file.isDirectory) {
            res.addAll(scanDirectory(file, type))
            continue
        }
        if (file.getFileType() != type) {
            continue
        }
        res.add(
            FileInfo(
                name = file.name,
                size = file.length(),
                isDirectory = file.isDirectory,
                type = file.getFileType(),
                lastModified = file.lastModified(),
                path = file.path.substring(AppConfig.BASE_DIR.length)
            )
        )
    }
    return res
}