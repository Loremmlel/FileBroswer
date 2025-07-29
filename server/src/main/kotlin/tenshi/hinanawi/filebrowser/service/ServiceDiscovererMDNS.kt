package tenshi.hinanawi.filebrowser.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.SERVER_PORT
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.math.log

class ServiceDiscovererMDNS {
  private val logger = LoggerFactory.getLogger(ServiceDiscovererMDNS::class.java)
  private var jmdns: JmDNS? = null

  fun start() = try {
    jmdns = JmDNS.create(InetAddress.getLocalHost())

    val serviceInfo = ServiceInfo.create(
      "_http._tcp.local.",
      "filebrowser-server",
      SERVER_PORT,
      "柚子的文件服务器"
    )
    jmdns?.registerService(serviceInfo)
    CoroutineScope(Dispatchers.IO).launch {
      delay(2000)
      logger.info("开始寻找服务")
      val services = jmdns?.list("_http._tcp.local.")
      services?.forEach { service ->
        logger.info("发现服务: ${service.name} - ${service.server}:${service.port}")
      } ?: logger.info("没有发现服务")
    }
    logger.info("mDNS服务已经注册: ${serviceInfo.name} 在端口: $SERVER_PORT")
  } catch (e: Exception) {
    logger.error("启动mDNS服务失败", e)
  }

  fun stop() {
    logger.info("停止mDNS服务")
    jmdns?.unregisterAllServices()
    jmdns?.close()
    jmdns = null
  }
}