package tenshi.hinanawi.filebrowser.platform

import io.ktor.utils.io.*
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement

class WebFileDownloader : FileDownloader {
  override suspend fun downloadFile(
    url: String,
    filename: String,
    inputChannel: ByteReadChannel?,
    contentLength: Long?
  ) {
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = url
    link.download = filename
    link.style.display = "none"
    document.body?.appendChild(link)
    link.click()
    document.body?.removeChild(link)
  }
}

actual fun createFileDownloader(): FileDownloader = WebFileDownloader()