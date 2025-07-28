package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.Serializable
import tenshi.hinanawi.filebrowser.util.currentTimeMillis

/**
 * 服务发现广播消息
 */
@Serializable
data class ServiceBroadcast(
  val serviceName: String = "FileBrowser",
  val version: String = "1.0.0",
  val host: String,
  val port: Int,
  val timestamp: Long = currentTimeMillis()
)

/**
 * 发现的服务信息
 */
@Serializable
data class DiscoveredService(
  val serviceName: String,
  val version: String,
  val host: String,
  val port: Int,
  val lastSeen: Long,
  val url: String = "http://$host:$port"
)

/**
 * 服务配置
 */
@Serializable
data class ServiceConfig(
  val autoDiscovery: Boolean = true,
  val manualHost: String? = null,
  val manualPort: Int? = null,
  val broadcastPort: Int = 8888,
  val discoveryTimeout: Long = 5000L
)

