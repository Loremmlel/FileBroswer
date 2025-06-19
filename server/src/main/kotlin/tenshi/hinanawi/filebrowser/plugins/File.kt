package tenshi.hinanawi.filebrowser.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.file() {
    routing {
        get("/file") {
            val path = call.pathParameters["path"]

        }
    }
}