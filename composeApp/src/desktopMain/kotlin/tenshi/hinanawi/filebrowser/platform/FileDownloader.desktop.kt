package tenshi.hinanawi.filebrowser.platform

import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser

class DesktopFileDownloader : FileDownloader {
  override suspend fun downloadFile(url: String, fileName: String, fileData: ByteArray) {
    try {
      // 使用 JFileChooser 让用户选择保存位置
      val fileChooser = JFileChooser().apply {
        dialogTitle = "保存文件"
        selectedFile = File(fileName)
        fileSelectionMode = JFileChooser.FILES_ONLY
      }
      
      val result = fileChooser.showSaveDialog(null)
      if (result == JFileChooser.APPROVE_OPTION) {
        val selectedFile = fileChooser.selectedFile
        
        // 写入文件
        FileOutputStream(selectedFile).use { outputStream ->
          outputStream.write(fileData)
        }
      } else {
        throw Exception("用户取消了下载")
      }
    } catch (e: Exception) {
      throw Exception("下载失败: ${e.message}")
    }
  }
}

actual fun createFileDownloader(): FileDownloader = DesktopFileDownloader()