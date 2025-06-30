package tenshi.hinanawi.filebrowser.platform

import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser

class DesktopFileDownloader : FileDownloader {
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
        // 使用 JFileChooser 让用户选择保存位置
        val fileChooser = JFileChooser().apply {
          dialogTitle = "保存文件"
          selectedFile = File(filename)
          fileSelectionMode = JFileChooser.FILES_ONLY
        }

        val result = fileChooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
          val selectedFile = fileChooser.selectedFile

          // 流式写入文件
          FileOutputStream(selectedFile).use { outputStream ->
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
        } else {
          return@withContext
        }
      } catch (e: Exception) {
        throw Exception("流式下载失败: ${e.message}")
      }
    }
  }
}

actual fun createFileDownloader(): FileDownloader = DesktopFileDownloader()