package tenshi.hinanawi.filebrowser

const val SERVER_PORT = 8080

val SERVER_HOST = if (getPlatform().name.contains("Android")) "10.0.2.2" else "127.0.0.1"

val SERVER_URL = "http://$SERVER_HOST:$SERVER_PORT"