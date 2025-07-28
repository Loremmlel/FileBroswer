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

