package tenshi.hinanawi.filebrowser

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "文件浏览器",
  ) {
    App()
  }
}