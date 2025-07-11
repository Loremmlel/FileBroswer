package tenshi.hinanawi.filebrowser.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

/**
 * 全局CORS处理插件
 * 开发阶段使用，设置全允许的CORS策略
 */
val GlobalCorsHandler = createApplicationPlugin("GlobalCorsHandler") {
  on(ResponseBodyReadyForSend) { call, _ ->
    call.response.headers.apply {
      append(HttpHeaders.AccessControlAllowOrigin, "*")

      append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD")

      append(
        HttpHeaders.AccessControlAllowHeaders,
        "Content-Type, Authorization, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers"
      )

      // 允许携带凭证（如果需要的话，但与 * 来源不兼容，开发时可以注释掉）
      // append(HttpHeaders.AccessControlAllowCredentials, "true")

      append(HttpHeaders.AccessControlMaxAge, "86400")
    }
  }

  // 处理预检请求
  on(CallSetup) { call ->
    if (call.request.httpMethod == HttpMethod.Options) {
      call.respond(HttpStatusCode.OK)
    }
  }
}

