package tenshi.hinanawi.filebrowser.util

import io.ktor.server.application.*
import io.ktor.server.response.*

fun ApplicationCall.contentTypeJson() = this.response.header("Content-Type", "application/json")
