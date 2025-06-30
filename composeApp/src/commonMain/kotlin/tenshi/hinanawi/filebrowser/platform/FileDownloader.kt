package tenshi.hinanawi.filebrowser.platform

interface FileDownloader {
  suspend fun downloadFile(url: String, fileName: String, fileData: ByteArray)
}

expect fun createFileDownloader(): FileDownloader