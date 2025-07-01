package tenshi.hinanawi.filebrowser.util


fun jsDateNow(): JsNumber = js("Date.now()")

actual fun currentTimeMillis(): Long = jsDateNow().toDouble().toLong()