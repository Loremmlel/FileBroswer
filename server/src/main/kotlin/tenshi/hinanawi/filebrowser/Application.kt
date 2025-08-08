package tenshi.hinanawi.filebrowser

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.config.DatabaseFactory
import tenshi.hinanawi.filebrowser.plugin.GlobalCorsHandler
import tenshi.hinanawi.filebrowser.plugin.GlobalExceptionHandler
import tenshi.hinanawi.filebrowser.plugin.RequestLogging
import tenshi.hinanawi.filebrowser.plugin.ResponseBodyLogging
import tenshi.hinanawi.filebrowser.route.*
import tenshi.hinanawi.filebrowser.service.ServiceDiscoverer
import tenshi.hinanawi.filebrowser.service.TranscodeService

fun main() {
  System.setProperty("io.ktor.development", "true")
  val transcoder = TranscodeService()
  val serviceDiscoverer = ServiceDiscoverer()

  val server = embeddedServer(
    CIO,
    port = SERVER_PORT,
    host = "0.0.0.0",
    module = {
      module(transcoder)
    }
  )
  server.monitor.apply {
    subscribe(ApplicationStarted) {
      serviceDiscoverer.start()
    }
    subscribe(ApplicationStopping) {
      transcoder.cleanup()
      serviceDiscoverer.stop()
    }
  }
  server.start(wait = true)
}

fun Application.module(
  transcoder: TranscodeService
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
    favorite()
    thumbnail()
    transcode(transcoder)
    video()
  }
}