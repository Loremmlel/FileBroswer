package tenshi.hinanawi.filebrowser.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.exception.ServiceException
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response

/**
 * 全局异常处理插件
 * 统一处理所有未捕获的异常，记录日志并返回标准错误响应
 */
val GlobalExceptionHandler = createApplicationPlugin("GlobalExceptionHandler") {
  val logger = LoggerFactory.getLogger("ExceptionHandler")

  on(CallFailed) { call, cause ->
    logger.error("Request failed: ${call.request.httpMethod.value} ${call.request.uri}", cause)

    // 如果响应还没有发送，发送错误响应
    if (!call.response.isCommitted) {
      try {
        call.respond(
          HttpStatusCode.InternalServerError,
          Response(500, Message.InternalServerError, null)
        )
      } catch (e: Exception) {
        logger.error("发送错误消息失败", e)
      }
    }
  }
}

/**
 * 路由级别的异常处理扩展函数
 * 用于包装路由处理逻辑，自动捕获异常并记录日志
 */
suspend inline fun ApplicationCall.safeExecute(
  crossinline block: suspend ApplicationCall.() -> Unit
) {
  val logger = LoggerFactory.getLogger("RouteHandler")
  try {
    block()
  } catch (e: ServiceException) {
    logger.warn("ServiceException异常: ${request.httpMethod.value} ${request.uri}", e)
    if (!response.isCommitted) {
      respond(
        HttpStatusCode.BadRequest,
        Response(400, e.serviceMessage.toClientMessage(), null)
      )
    }
  } catch (e: ClosedByteChannelException) {
    // 单独处理，不然日志一大坨
    logger.info("客户端断开视频链接: ip:${request.local.remoteAddress}, ${e.message}")
  } catch (e: Exception) {
    logger.error("路由执行失败: ${request.httpMethod.value} ${request.uri}", e)
    if (!response.isCommitted) {
      respond(
        HttpStatusCode.InternalServerError,
        Response(500, Message.InternalServerError, null)
      )
    }
  }
}

/**
 * 带自定义错误处理的安全执行扩展函数
 */
suspend inline fun ApplicationCall.safeExecute(
  crossinline onError: suspend ApplicationCall.(Exception) -> Unit,
  crossinline block: suspend ApplicationCall.() -> Unit
) {
  val logger = LoggerFactory.getLogger("RouteHandler")
  try {
    block()
  } catch (e: Exception) {
    logger.error("路由执行失败: ${request.httpMethod.value} ${request.uri}", e)
    onError(e)
  }
}