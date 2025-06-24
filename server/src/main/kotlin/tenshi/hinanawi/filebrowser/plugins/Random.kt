package tenshi.hinanawi.filebrowser.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.model.parseFileType

fun Application.random() {
    routing {
        get("/random") {
            try {
                val type = call.queryParameters["type"]?.parseFileType() ?: FileType.Video
                val path = call.queryParameters["path"]
            } catch (e: Exception) {

            }
        }
    }
}