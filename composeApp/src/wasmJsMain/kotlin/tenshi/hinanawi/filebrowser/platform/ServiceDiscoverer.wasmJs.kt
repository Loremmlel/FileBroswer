package tenshi.hinanawi.filebrowser.platform

import kotlinx.browser.window

actual fun createServiceDiscoverer(): ServiceDiscoverer = WasmJsServiceDiscoverer()

private class WasmJsServiceDiscoverer : ServiceDiscoverer {
  override fun startDiscovery(onServiceFound: (host: String, port: Int) -> Unit) {
    // 浏览器环境不支持mDNS服务发现
    // 这是一个已知的平台限制
//    window.alert("浏览器不支持mDNS，请手动填写服务器IP地址!!!")
  }

  override fun stopDiscovery() {
    // 无操作
  }
}