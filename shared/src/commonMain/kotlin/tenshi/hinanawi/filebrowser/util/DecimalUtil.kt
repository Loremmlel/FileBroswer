package tenshi.hinanawi.filebrowser.util

import kotlin.math.ln
import kotlin.math.pow

fun Long.formatFileSize(): String {
    if (this <= 0) {
        return "0 Bytes"
    }
    val units = (ln(this.toDouble()) / ln(1024.0)).toInt()
    val digitGroup = listOf("Bytes", "KB", "MB", "GB", "TB")
    return "${(this / 1024.0.pow(units)).toFixed(1)} ${digitGroup[units]}"
}

fun Double.toFixed(digit: Int): String {
    val string = this.toString()
    return if (string.contains(".")) {
        val index = string.indexOf(".")
        string.substring(0, index + digit + 1)
    } else {
        string
    }
}