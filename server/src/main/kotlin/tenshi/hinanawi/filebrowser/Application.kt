package tenshi.hinanawi.filebrowser

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import tenshi.hinanawi.filebrowser.route.files
import tenshi.hinanawi.filebrowser.route.image
import tenshi.hinanawi.filebrowser.route.random

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = Application::module
    )
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    files()
    random()
    image()
}