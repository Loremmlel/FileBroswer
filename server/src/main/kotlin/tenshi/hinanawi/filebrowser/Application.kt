package tenshi.hinanawi.filebrowser

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.routing
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
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)

    allowHeader(HttpHeaders.Authorization)

    anyHost() // @TODO: Don't do this in production if possible. Try to limit it.

    allowSameOrigin = true
    allowCredentials = true
  }
  routing {
    files()
    random()
    image()
  }
}