package tenshi.hinanawi.filebrowser.util

import io.ktor.server.response.header
import io.ktor.server.routing.RoutingCall

fun RoutingCall.contentTypeJson() = this.response.header("Content-Type", "application/json")
