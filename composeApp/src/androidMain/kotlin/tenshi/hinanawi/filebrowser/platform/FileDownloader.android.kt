package tenshi.hinanawi.filebrowser.platform

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidFileDownloader(private val context: Context) : FileDownloader {
  override suspend fun downloadFile(
    url: String,
    filename: String,
    inputChannel: ByteReadChannel?,
    contentLength: Long?
  ) {
    withContext(Dispatchers.IO) {
      try {
        if (inputChannel == null) {
          throw Exception("输入流为空")
        }
        // 获取下载目录
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, filename)

        // 流式写入文件
        FileOutputStream(file).use { outputStream ->
          val buffer = ByteArray(8192) // 8KB 缓冲区
          while (!inputChannel.isClosedForRead) {
            val bytesRead = inputChannel.readAvailable(buffer)
            if (bytesRead > 0) {
              outputStream.write(buffer, 0, bytesRead)
            } else if (bytesRead == -1) {
              break
            }
          }
        }

        // 添加文件到下载数据库系统
        val contentValues = ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, filename)
          put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
          resolver.openOutputStream(it)?.use { outputStream ->
            // 重新读取文件内容写入到MediaStore
            file.inputStream().use { inputStream ->
              inputStream.copyTo(outputStream)
            }
          }
          contentValues.clear()
          contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
          resolver.update(it, contentValues, null, null)
        }
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "下载成功, 文件存储到${file.path}", Toast.LENGTH_LONG).show()
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        throw Exception("流式下载失败: ${e.message}")
      }
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