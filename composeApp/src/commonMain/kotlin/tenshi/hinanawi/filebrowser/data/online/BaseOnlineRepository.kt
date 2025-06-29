package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import tenshi.hinanawi.filebrowser.exception.ApiException
import tenshi.hinanawi.filebrowser.model.ResponseWithoutData

abstract class BaseOnlineRepository {
  protected val client = _client

  companion object {
    private val _client = HttpClient {
      install(ContentNegotiation) {
        json(Json {
          // 呵呵，纠结我两个多小时的问题，泛型类和kotlinx.serialization序列化器匹配，就这么丑陋的解决了
          // 响应拦截器不关心类型，所以创建一个没有data的Response即可
          // 然后启用忽略未知键，就可以了，具体类型在后面请求拿到
          ignoreUnknownKeys = true
        })
      }
      HttpResponseValidator {
        validateResponse { response ->
          val headers = response.headers
          val contentType = headers["Content-Type"]
          if (contentType != "application/json") {
            return@validateResponse
          }
          val body = response.body<ResponseWithoutData>()
          if (body.code in 400..599) {
            throw ApiException(body.code, body.message)
          }
        }
      }
    }
  }
}