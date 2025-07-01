package tenshi.hinanawi.filebrowser.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import io.ktor.util.*
import tenshi.hinanawi.filebrowser.config.AppConfig
import tenshi.hinanawi.filebrowser.model.Message
import tenshi.hinanawi.filebrowser.model.Response
import java.io.File
import java.nio.file.Paths

internal val ValidatedFileKey = AttributeKey<File>("ValidatedFileKey")

internal val PathValidator = createRouteScopedPlugin("PathValidator") {
  on(CallSetup) { call ->
    val path = call.request.queryParameters["path"]

    // 验证逻辑 1: 路径参数是否存在且格式正确
    if (path == null || !path.startsWith('/')) {
      call.respond(
        HttpStatusCode.BadRequest,
        Response(400, Message.FilesNotFound, null)
      )
      return@on
    }
    try {
      // 验证逻辑 2: 路径规范化和安全检查
      val normalizedPath = Paths.get(AppConfig.basePath, path).normalize()
      if (!normalizedPath.startsWith(AppConfig.basePath)) {
        call.respond(
          HttpStatusCode.Forbidden,
          Response(403, Message.FilesForbidden, null)
        )
        return@on
      }

      // 3. 如果所有验证都通过，将 File 对象放入 attributes 中
      val file = normalizedPath.toFile()
      if (!file.exists()) {
        call.respond(
          HttpStatusCode.NotFound,
          Response(404, Message.FilesNotFound, null)
        )
        return@on
      }
      call.attributes.put(ValidatedFileKey, file)
    } catch (e: Exception) {
      call.respond(
        HttpStatusCode.InternalServerError,
        Response(500, Message.InternalServerError, null)
      )
      call.logBusinessOperation("PathValidator", "PathValidationFailed: $e ${e.message}")
      return@on
    }
  }
}