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
import tenshi.hinanawi.filebrowser.util.TranscodeManager

fun main() {
  System.setProperty("io.ktor.development", "true")
  val transcodeManager = TranscodeManager()
  embeddedServer(
    Netty,
    port = SERVER_PORT,
    host = "0.0.0.0",
    module = {
      module(transcodeManager)
    }
  )
    .start(wait = true)
    .monitor.subscribe(ApplicationStopping) {
      transcodeManager.shutdown()
    }
}

fun Application.module(
  transcodeManager: TranscodeManager
) {
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
    transcode(transcodeManager)
    video()
    favorite()
    thumbnail()
  }
}