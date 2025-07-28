package tenshi.hinanawi.filebrowser.platform

interface ServiceDiscoverer {
  fun startDiscovery(onServiceFound: (host: String, port: Int) -> Unit)
  fun stopDiscovery()
}

expect fun createServiceDiscoverer(): ServiceDiscoverer