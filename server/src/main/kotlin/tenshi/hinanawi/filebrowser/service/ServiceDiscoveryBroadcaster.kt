package tenshi.hinanawi.filebrowser.service

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.SERVER_PORT
import tenshi.hinanawi.filebrowser.model.ServiceBroadcast
import java.net.*

/**
 * 服务发现广播器
 * 定时向局域网广播服务信息
 */
class ServiceDiscoveryBroadcaster(
  private val broadcastPort: Int = 8888,
  private val broadcastInterval: Long = 3000L // 3秒广播一次
) {
  private val logger = LoggerFactory.getLogger("business")
  private val json = Json { ignoreUnknownKeys = true }
  private var broadcastJob: Job? = null
  private val broadcastScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private var socket: DatagramSocket? = null

  fun start() {
    if (broadcastJob?.isActive == true) {
      logger.info("服务发现广播器已经在运行")
      return
    }

    broadcastJob = broadcastScope.launch {
      try {
        socket = DatagramSocket()
        socket?.broadcast = true

        val localHost = getLocalIPAddress()
        logger.info("开始广播服务发现信息: $localHost:$SERVER_PORT")

        while (isActive) {
          try {
            broadcastService(localHost)
            delay(broadcastInterval)
          } catch (e: Exception) {
            logger.error("广播服务信息失败", e)
            delay(broadcastInterval)
          }
        }
      } catch (e: Exception) {
        logger.error("启动服务发现广播器失败", e)
      }
    }
  }

  fun stop() {
    logger.info("停止服务发现广播器")
    broadcastJob?.cancel()
    socket?.close()
    socket = null
  }

  private fun broadcastService(host: String) {
    val broadcast = ServiceBroadcast(
      host = host,
      port = SERVER_PORT
    )

    val message = json.encodeToString(broadcast)
    val data = message.toByteArray()

    // 广播到所有网络接口
    val broadcastAddresses = getBroadcastAddresses()

    for (broadcastAddress in broadcastAddresses) {
      try {
        val packet = DatagramPacket(
          data,
          data.size,
          InetAddress.getByName(broadcastAddress),
          broadcastPort
        )
        socket?.send(packet)
        logger.debug("广播服务信息到: $broadcastAddress:$broadcastPort")
      } catch (e: Exception) {
        logger.debug("广播到 $broadcastAddress 失败: ${e.message}")
      }
    }
  }

  private fun getLocalIPAddress(): String {
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp) continue

        val addresses = networkInterface.inetAddresses
        while (addresses.hasMoreElements()) {
          val address = addresses.nextElement()
          if (address is Inet4Address && !address.isLoopbackAddress) {
            return address.hostAddress
          }
        }
      }
    } catch (e: Exception) {
      logger.error("获取本地IP地址失败", e)
    }
    return "127.0.0.1"
  }

  private fun getBroadcastAddresses(): List<String> {
    val broadcastAddresses = mutableListOf<String>()

    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp) continue

        for (interfaceAddress in networkInterface.interfaceAddresses) {
          val broadcast = interfaceAddress.broadcast
          if (broadcast != null) {
            broadcastAddresses.add(broadcast.hostAddress)
          }
        }
      }
    } catch (e: Exception) {
      logger.error("获取广播地址失败", e)
    }

    // 如果没有找到广播地址，使用默认的
    if (broadcastAddresses.isEmpty()) {
      broadcastAddresses.add("255.255.255.255")
    }

    return broadcastAddresses
  }
}

