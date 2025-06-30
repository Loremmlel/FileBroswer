package tenshi.hinanawi.filebrowser.platform

import kotlinx.browser.document
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.collections.forEachIndexed

class WebFileDownloader : FileDownloader {
  override suspend fun downloadFile(url: String, fileName: String, fileData: ByteArray) {
    // 将 ByteArray 转换为 Uint8Array
    val uint8Array = Uint8Array(fileData.size)
    fileData.forEachIndexed { index, byte ->
        uint8Array[index] = byte
    }
    
    // 创建 Blob
    val blob = Blob(uint8Array.toJsArray(), BlobPropertyBag(type = "application/octet-stream"))
    
    // 创建下载链接
    val blobUrl = URL.createObjectURL(blob)
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = blobUrl
    link.download = fileName
    link.style.display = "none"
    
    // 触发下载
    document.body?.appendChild(link)
    link.click()
    document.body?.removeChild(link)
    
    // 清理 URL
    URL.revokeObjectURL(blobUrl)
  }
}

fun Uint8Array.toJsArray(): JsArray<JsAny?> {
  val jsArray = JsArray<JsAny?>()
  for (i in 0 until this.length) {
      jsArray[i] = this[i].toJsReference()
  }
  return jsArray
}


actual fun createFileDownloader(): FileDownloader = WebFileDownloader()