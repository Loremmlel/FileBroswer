package tenshi.hinanawi.filebrowser.service

import kotlinx.io.IOException
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

class ServiceDiscoverer {
  private val logger = LoggerFactory.getLogger(ServiceDiscoverer::class.java)

  @Volatile
  private var running = false
  private var broadcastThread: Thread? = null
  private var socket: DatagramSocket? = null

  companion object {
    private const val BROADCAST_PORT = 23333
    private const val HANDSHAKE_MESSAGE = "SHIKIYUZU CIALLO"
    private const val BROADCAST_INTERVAL = 3000L
  }
  fun start() {
    if (running) {
      logger.warn("ServiceDiscoverer 正在运行，却尝试再次开启")
      return
    }
    running = true
    broadcastThread = Thread {
      try {
        socket = DatagramSocket().also { it.broadcast = true }
        logger.info("Service Discoverer启动，监听${BROADCAST_PORT}端口")
        while (running) {
          try {
            val ipAddress = getLocalIpAddress()
            if (ipAddress == null) {
              logger.warn("无法找到合适的本地IP地址")
              Thread.sleep(BROADCAST_INTERVAL)
              continue
            }
            val message = "$HANDSHAKE_MESSAGE:$ipAddress"
            val sendData = message.toByteArray()
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddress, BROADCAST_PORT)
            socket?.send(sendPacket)
            logger.debug("📢广播发送: $message")
            Thread.sleep(BROADCAST_INTERVAL)
          } catch (_: InterruptedException) {
            logger.info("广播线程被打断，停止中")
            Thread.currentThread().interrupt()
          } catch (e: IOException) {
            if (running) {
              logger.error("发送广播包时错误: ", e)
            }
          }
        }
      } catch (e: SocketException) {
        logger.error("创建UDP套接字失败: ", e)
      } finally {
        socket?.close()
        logger.info("Service Discoverer 停止")
      }
    }

    broadcastThread?.name = "ServiceDiscovererThread"
    broadcastThread?.isDaemon = true
    broadcastThread?.start()
  }

  fun stop() {
    running = false
    broadcastThread?.interrupt()
    socket?.close()
    try {
      broadcastThread?.join(1000)
    } catch (_: InterruptedException) {
      logger.warn("等待广播线程停止的过程中被打断")
      Thread.currentThread().interrupt()
    }
    logger.info("停止信号已发送给广播线程")
  }

  private fun getLocalIpAddress(): String? {
    try {
      val networkInterfaces = NetworkInterface.getNetworkInterfaces()
      while (networkInterfaces.hasMoreElements()) {
        val networkInterface = networkInterfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp || networkInterface.isVirtual) {
          continue
        }
        val inetAddresses = networkInterface.inetAddresses
        while (inetAddresses.hasMoreElements()) {
          val inetAddress = inetAddresses.nextElement()
          if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress) {
            return inetAddress.hostAddress
          }
        }
      }
    } catch (e: SocketException) {
      logger.error("无法获取网络接口", e)
    }
    return null
  }
}