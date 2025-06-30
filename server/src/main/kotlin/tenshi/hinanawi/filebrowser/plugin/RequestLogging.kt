package tenshi.hinanawi.filebrowser.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 请求日志插件
 * 记录每次请求的详细信息，包括请求方法、地址、参数和响应信息
 */
val RequestLogging = createApplicationPlugin("RequestLogging") {
  val logger = LoggerFactory.getLogger("RequestLogger")
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  on(CallSetup) { call ->
    val startTime = System.currentTimeMillis()
    call.attributes.put(RequestStartTimeKey, startTime)

    // 记录请求开始信息
    val requestInfo = buildString {
      append("REQUEST START - ")
      append("${call.request.httpMethod.value} ${call.request.uri}")

      // 记录查询参数
      val queryParams = call.request.queryParameters
      if (!queryParams.isEmpty()) {
        append(" | Query: ")
        queryParams.entries().joinToString(", ") { (key, values) ->
          "$key=${values.joinToString(",")}"
        }.let { append(it) }
      }

      // 记录请求头（选择性记录重要的头）
      val importantHeaders = listOf(
        HttpHeaders.ContentType,
        HttpHeaders.ContentLength,
        HttpHeaders.UserAgent,
        HttpHeaders.Authorization,
        HttpHeaders.Range
      )
      val headers = importantHeaders.mapNotNull { headerName ->
        call.request.headers[headerName]?.let { "$headerName=$it" }
      }
      if (headers.isNotEmpty()) {
        append(" | Headers: ${headers.joinToString(", ")}")
      }

      append(" | Client: ${call.request.origin.remoteHost}")
      append(" | Time: ${LocalDateTime.now().format(dateFormatter)}")
    }

    logger.info(requestInfo)

    // 记录请求体信息（不实际读取内容，避免干扰正常处理）
    if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
      try {
        val contentType = call.request.contentType()
        val contentLength = call.request.headers[HttpHeaders.ContentLength]
        logger.info("REQUEST BODY INFO - ${call.request.httpMethod.value} ${call.request.uri} | Content-Type: $contentType | Content-Length: $contentLength")
      } catch (e: Exception) {
        // 忽略content-type解析错误
      }
    }
  }

  on(ResponseSent) { call ->
    val startTime = call.attributes.getOrNull(RequestStartTimeKey) ?: System.currentTimeMillis()
    val duration = System.currentTimeMillis() - startTime

    // 记录响应信息
    val responseInfo = buildString {
      append("REQUEST END - ")
      append("${call.request.httpMethod.value} ${call.request.uri}")
      append(" | Status: ${call.response.status()}")
      append(" | Duration: ${duration}ms")

      // 记录响应头
      val responseHeaders = call.response.headers.allValues()
      val importantResponseHeaders = listOf(
        HttpHeaders.ContentType,
        HttpHeaders.ContentLength,
        HttpHeaders.Location
      )
      val headers = importantResponseHeaders.mapNotNull { headerName ->
        responseHeaders[headerName]?.firstOrNull()?.let { "$headerName=$it" }
      }
      if (headers.isNotEmpty()) {
        append(" | Headers: ${headers.joinToString(", ")}")
      }

      append(" | Time: ${LocalDateTime.now().format(dateFormatter)}")
    }

    logger.info(responseInfo)

    // 尝试记录响应体（仅对JSON响应且状态码不是2xx时记录，避免日志过大）
    try {
      val contentType = call.response.headers[HttpHeaders.ContentType]
      val status = call.response.status()

      if (contentType?.contains("application/json") == true &&
        (status?.value?.let { it < 200 || it >= 300 } == true)
      ) {

        // 对于错误响应，我们可能想要记录响应体
        // 注意：在ResponseSent阶段，响应体已经发送，无法直接读取
        // 这里只是一个占位符，实际实现可能需要在更早的阶段拦截
        logger.info("RESPONSE BODY - ${call.request.httpMethod.value} ${call.request.uri} | Status: $status | Content-Type: $contentType")
      }
    } catch (e: Exception) {
      // 忽略响应体记录错误
    }
  }
}

/**
 * 用于存储请求开始时间的属性键
 */
private val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")


/**
 * 扩展函数，用于在路由中记录特定的业务日志
 */
fun ApplicationCall.logBusinessOperation(operation: String, details: String? = null) {
  val logger = LoggerFactory.getLogger("BusinessLogger")
  val logMessage = buildString {
    append("BUSINESS - $operation")
    append(" | ${request.httpMethod.value} ${request.uri}")
    details?.let { append(" | Details: $it") }
    append(" | Client: ${request.origin.remoteHost}")
    append(" | Time: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}")
  }
  logger.info(logMessage)
}