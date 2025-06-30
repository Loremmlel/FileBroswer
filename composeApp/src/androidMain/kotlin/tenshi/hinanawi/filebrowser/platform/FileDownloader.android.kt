package tenshi.hinanawi.filebrowser.platform

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

class AndroidFileDownloader(private val context: Context) : FileDownloader {
  override suspend fun downloadFile(url: String, fileName: String, fileData: ByteArray) {
    try {
      // 获取下载目录
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val file = File(downloadsDir, fileName)
      
      // 写入文件
      FileOutputStream(file).use { outputStream ->
        outputStream.write(fileData)
      }


      // 添加文件到下载数据库系统
      val contentValues = android.content.ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

      uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
          outputStream.write(fileData)
        }
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(it, contentValues, null, null)
      }
    } catch (e: Exception) {
      throw Exception("下载失败: ${e.message}")
    }
  }
}

// 需要在 MainActivity 中设置 context
private var applicationContext: Context? = null

fun setApplicationContext(context: Context) {
  applicationContext = context
}

actual fun createFileDownloader(): FileDownloader {
  return AndroidFileDownloader(
    applicationContext ?: throw IllegalStateException("Application context not set")
  )
}