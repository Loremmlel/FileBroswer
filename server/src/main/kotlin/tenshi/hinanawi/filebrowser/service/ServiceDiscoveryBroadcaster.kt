package tenshi.hinanawi.filebrowser.service

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tenshi.hinanawi.filebrowser.SERVER_PORT
import tenshi.hinanawi.filebrowser.model.ServiceBroadcast
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 服务发现广播器
 * 定时向局域网广播服务信息
 */
class ServiceDiscoveryBroadcaster(
  private val broadcastPort: Int = 8888,
  private val broadcastInterval: Long = 3000L // 3秒广播一次
) {
  private val logger = LoggerFactory.getLogger(ServiceDiscoveryBroadcaster::class.java)
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

        logger.info("开始广播服务发现信息")

        while (isActive) {
          try {
            broadcastService()
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

  private fun broadcastService() = try {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    while (interfaces.hasMoreElements()) {
      val networkInterface = interfaces.nextElement()
      if (networkInterface.isLoopback || !networkInterface.isUp) continue

      for (interfaceAddress in networkInterface.interfaceAddresses) {
        val broadcast = interfaceAddress.broadcast ?: continue
        val address = interfaceAddress.address

        if (address is Inet4Address && !address.isLoopbackAddress) {
          val serviceBroadcast = ServiceBroadcast(
            host = address.hostAddress,
            port = SERVER_PORT
          )
          val message = json.encodeToString(serviceBroadcast)
          val data = message.toByteArray()

          try {
            val packet = DatagramPacket(
              data,
              data.size,
              broadcast,
              broadcastPort
            )
            socket?.send(packet)
            logger.debug("广播服务信息到: ${broadcast.hostAddress}:$broadcastPort on host ${address.hostAddress}")
          } catch (e: Exception) {
            logger.debug("广播到 ${broadcast.hostAddress} 失败: ${e.message}")
          }
        }
      }
    }
  } catch (e: Exception) {
    logger.error("获取网络接口失败", e)
  }
}

