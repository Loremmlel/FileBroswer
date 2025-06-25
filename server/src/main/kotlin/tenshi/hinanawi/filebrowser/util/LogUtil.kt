package tenshi.hinanawi.filebrowser.util

import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*

fun Logger.requestError(call: RoutingCall, exception: Exception) =
    this.error("接口${call.request.path()}请求错误: ${exception.message}")
