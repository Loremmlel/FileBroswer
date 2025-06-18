package tenshi.hinanawi.filebrowser

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform