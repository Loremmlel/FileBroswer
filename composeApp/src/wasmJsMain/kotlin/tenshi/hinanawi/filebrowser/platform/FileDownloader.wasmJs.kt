package tenshi.hinanawi.filebrowser.platform

import io.ktor.utils.io.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.files.Blob

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