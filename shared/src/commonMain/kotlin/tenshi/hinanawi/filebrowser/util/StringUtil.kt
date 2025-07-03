package tenshi.hinanawi.filebrowser.util

fun String.truncate(maxLength: Int): String {
  return if (this.length > maxLength) {
    this.substring(0, maxLength) + "..."
  } else {
    this
  }
}