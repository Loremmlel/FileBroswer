package tenshi.hinanawi.filebrowser

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.DatabaseFactory
import tenshi.hinanawi.filebrowser.plugin.GlobalCorsHandler
import tenshi.hinanawi.filebrowser.plugin.GlobalExceptionHandler
import tenshi.hinanawi.filebrowser.plugin.RequestLogging
import tenshi.hinanawi.filebrowser.plugin.ResponseBodyLogging
import tenshi.hinanawi.filebrowser.route.*

fun main() {
  System.setProperty("io.ktor.development", "true")
  embeddedServer(
    Netty,
    port = SERVER_PORT,
    host = "0.0.0.0",
    module = {
      module()
    }
  )
    .start(wait = true)
    .monitor.subscribe(ApplicationStopping) {
    }
}

fun Application.module() {
  DatabaseFactory.init()
  install(GlobalCorsHandler)
  install(RequestLogging)
  install(ResponseBodyLogging)
  install(GlobalExceptionHandler)
  install(ContentNegotiation) {
    json()
  }

  routing {
    files()
    random()
    image()
    favorite()
    thumbnail()
  }
}