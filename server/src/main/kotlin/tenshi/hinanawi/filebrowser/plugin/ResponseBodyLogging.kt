package tenshi.hinanawi.filebrowser.plugin

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * 响应体日志记录插件
 * 专门用于记录JSON响应的内容
 */
val ResponseBodyLogging = createApplicationPlugin("ResponseBodyLogging") {
  val logger = LoggerFactory.getLogger("ResponseBodyLogger")
  val json = Json { prettyPrint = false }

  on(ResponseBodyReadyForSend) { call, body ->
    try {
      val contentType = call.response.headers[HttpHeaders.ContentType]
      val status = call.response.status()

      // 只记录JSON响应
      if (contentType?.contains("application/json") == true) {
        val bodyString = when (body) {
          is TextContent -> body.text
          else -> {
            try {
              json.encodeToString(body)
            } catch (_: Exception) {
              body.toString()
            }
          }
        }

        // 限制日志长度，避免过大的响应体
        val logBody = if (bodyString.length > 1000) {
          bodyString.take(1000) + "...${bodyString.length - 1000}(truncated)"
        } else {
          bodyString
        }

        logger.info("RESPONSE BODY - ${call.request.httpMethod.value} ${call.request.uri} | Status: $status | Body: $logBody")
      }
    } catch (e: Exception) {
      // 忽略日志记录错误，不影响正常响应
      logger.debug("Failed to log response body", e)
    }
  }
}