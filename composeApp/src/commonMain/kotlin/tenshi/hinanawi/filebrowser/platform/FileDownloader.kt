package tenshi.hinanawi.filebrowser.platform

import io.ktor.utils.io.*

interface FileDownloader {
  suspend fun downloadFile(url: String, filename: String, inputChannel: ByteReadChannel? = null, contentLength: Long? = null)
}

expect fun createFileDownloader(): FileDownloader