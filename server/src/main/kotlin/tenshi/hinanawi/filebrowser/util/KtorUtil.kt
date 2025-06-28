package tenshi.hinanawi.filebrowser.util

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun RoutingCall.contentTypeJson() = this.response.header("Content-Type", "application/json")
