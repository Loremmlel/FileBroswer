package tenshi.hinanawi.filebrowser.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tenshi.hinanawi.filebrowser.util.ErrorHandler
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

actual fun createServiceDiscoverer(): ServiceDiscoverer = DesktopServiceDiscoverer()

private class DesktopServiceDiscoverer() : ServiceDiscoverer {
  private var jmdns: JmDNS? = null
  private var serviceListener: ServiceListener? = null
  private val serviceType = "_http._tcp.local."

  override fun startDiscovery(onServiceFound: (String, Int) -> Unit) = try {
     jmdns = JmDNS.create(InetAddress.getLocalHost())

    serviceListener = object : ServiceListener {
      override fun serviceAdded(event: ServiceEvent?) {
        jmdns?.requestServiceInfo(event?.type, event?.name, 1000)
      }

      override fun serviceRemoved(event: ServiceEvent?) {
        println("服务已移除: ${event?.info?.name}")
      }

      override fun serviceResolved(event: ServiceEvent?) {
        val hostAddress = event?.info?.hostAddresses?.firstOrNull()
        if (hostAddress != null) {
          println("服务已发现: ${event.info.name} at $hostAddress:${event.info.port}")
          onServiceFound(hostAddress, event.info.port)
        }
      }
    }

    jmdns?.addServiceListener(serviceType, serviceListener)
    println("桌面端 mDNS 监听器已启动...")
  } catch (e: Exception) {
    CoroutineScope(Dispatchers.IO).launch {
      ErrorHandler.handleException(e)
    }
    Unit
  }

  override fun stopDiscovery() {
    serviceListener?.let { jmdns?.removeServiceListener(serviceType, it) }
    jmdns?.close()
    jmdns = null
    serviceListener = null
    println("桌面端 mDNS 监听器已停止。")
  }
}