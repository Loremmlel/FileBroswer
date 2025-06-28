package tenshi.hinanawi.filebrowser.data.online

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import tenshi.hinanawi.filebrowser.exception.ApiException
import tenshi.hinanawi.filebrowser.model.Response

abstract class BaseOnlineRepository {
    protected val client = _client
    companion object {
        private val _client = HttpClient {
            install(ContentNegotiation) {
                json()
            }
            HttpResponseValidator {
                validateResponse { response ->
                    val headers = response.headers
                    val contentType = headers["Content-Type"]
                    if (contentType != "application/json") {
                        return@validateResponse
                    }
                    val body = response.body<Response<*>>()
                    if (body.code in 400..599) {
                        throw ApiException(body.code, body.message)
                    }
                }
            }
        }
    }
}