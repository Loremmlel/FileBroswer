package tenshi.hinanawi.filebrowser.platform

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import tenshi.hinanawi.filebrowser.MainActivity

actual fun createServiceDiscoverer(): ServiceDiscoverer = AndroidServiceDiscoverer(MainActivity.context)

private class AndroidServiceDiscoverer(context: Context) : ServiceDiscoverer {
  private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
  private var discoveryListener: NsdManager.DiscoveryListener? = null
  private val serviceType = "_http._tcp"

  override fun startDiscovery(onServiceFound: (String, Int) -> Unit) {
    discoveryListener = object : NsdManager.DiscoveryListener {
      override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
        println("启动发现失败: Error code: $errorCode")
        nsdManager.stopServiceDiscovery(this)
      }

      override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
        println("停止发现失败: Error code: $errorCode")
      }

      override fun onDiscoveryStarted(serviceType: String?) {
        println("Android NSD 服务发现已启动")
      }

      override fun onDiscoveryStopped(serviceType: String?) {
        println("Android NSD 服务发现已停止")
      }

      override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
        println("发现服务: ${serviceInfo?.serviceName}")
        // 发现服务后，需要解析它来获取IP和端口
        nsdManager.resolveService(serviceInfo, createResolveListener(onServiceFound))
      }

      override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        println("服务丢失: ${serviceInfo?.serviceName}")
      }
    }

    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
  }

  private fun createResolveListener(onServiceFound: (host: String, port: Int) -> Unit): NsdManager.ResolveListener {
    return object : NsdManager.ResolveListener {
      override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        println("解析服务失败: Error code: $errorCode")
      }

      override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        println("服务解析成功: ${serviceInfo.serviceName}")
        val host = serviceInfo.host.hostAddress
        val port = serviceInfo.port
        if (host != null) {
          onServiceFound(host, port)
        }
      }
    }
  }

  override fun stopDiscovery() {
    discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
    discoveryListener = null
    println("Android NSD 服务发现已停止。")
  }
}