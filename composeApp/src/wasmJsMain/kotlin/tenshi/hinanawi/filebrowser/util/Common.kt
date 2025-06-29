package tenshi.hinanawi.filebrowser.util

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

internal suspend fun loadRes(url: String): ArrayBuffer = window.fetch(url).await<Response>().arrayBuffer().await()

internal fun ArrayBuffer.toByteArray(): ByteArray {
  val source = Int8Array(this, 0, byteLength)
  return jsInt8ArrayToKtByteArray(source)
}

internal fun jsInt8ArrayToKtByteArray(array: Int8Array): ByteArray {
  val size = array.length

  @OptIn(UnsafeWasmMemoryApi::class)
  return withScopedMemoryAllocator { allocator ->
    val memBuffer = allocator.allocate(size)
    val dstAddress = memBuffer.address.toInt()
    jsExportInt8ArrayToWasm(array, size, dstAddress)
    ByteArray(size) { i -> (memBuffer + i).loadByte() }
  }
}

@JsFun(
  """
    (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
internal external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)
