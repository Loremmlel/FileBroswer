package tenshi.hinanawi.filebrowser.route

import io.ktor.http.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenshi.hinanawi.filebrowser.plugin.PathValidator
import tenshi.hinanawi.filebrowser.plugin.ValidatedFileKey
import tenshi.hinanawi.filebrowser.plugin.safeExecute

fun Route.download() {
    route("/download") {
        install(PathValidator)
        install(PartialContent)
        get {
            call.safeExecute {
                val file = attributes[ValidatedFileKey]
                response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter(ContentDisposition.Parameters.FileName, file.name)
                        .toString()
                )
                response.header(
                    HttpHeaders.ContentLength,
                    file.length().toString()
                )
                respondFile(file)
            }
        }
    }
}