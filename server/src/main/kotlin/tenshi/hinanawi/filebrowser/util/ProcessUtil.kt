package tenshi.hinanawi.filebrowser.util

import org.slf4j.Logger
import java.util.concurrent.TimeUnit

/**
 * 执行命令并等待完成，自动处理输出和错误流
 * @param command 要执行的命令
 * @param timeout 超时时间
 * @param logger 用于记录日志的Logger
 * @param processName 进程名称（用于日志）
 * @return Pair<是否成功完成, 进程输出>
 */
fun executeCommand(
  command: List<String>,
  timeout: Long,
  unit: TimeUnit,
  logger: Logger,
  processName: String
): Boolean {
  try {
    val process = ProcessBuilder(command)
      .redirectErrorStream(true)
      .start()

    val outputConsumer = Thread {
      process.inputStream.bufferedReader().use { reader ->
        val output = StringBuilder()
        reader.forEachLine { line ->
          output.appendLine(line)
        }
      }
    }
    outputConsumer.start()

    val completed = process.waitFor(timeout, unit)
    outputConsumer.join(1000)

    if (!completed) {
      logger.warn("$processName 执行超时")
      return false
    }

    if (process.exitValue() != 0) {
      logger.warn("$processName 执行失败，退出码: ${process.exitValue()}")
      return false
    }

    return true
  } catch (e: Exception) {
    logger.warn("执行 $processName 失败", e.message)
    return false
  }
}

/**
 * 执行命令并返回输出内容
 * @param command 要执行的命令
 * @param timeout 超时时间
 * @param unit 时间单位
 * @param logger 用于记录日志的Logger
 * @param processName 进程名称（用于日志）
 * @return 进程输出内容，失败返回null
 */
fun executeAndGetOutput(
  command: List<String>,
  timeout: Long,
  unit: TimeUnit,
  logger: Logger,
  processName: String
): String? {
  try {
    val process = ProcessBuilder(command)
      .redirectErrorStream(true)
      .start()

    val output = StringBuilder()
    val outputConsumer = Thread {
      process.inputStream.bufferedReader().use { reader ->
        reader.forEachLine { line ->
          output.appendLine(line)
        }
      }
    }
    outputConsumer.start()

    val completed = process.waitFor(timeout, unit)
    outputConsumer.join(1000)

    if (!completed) {
      logger.warn("$processName 执行超时")
      return null
    }

    if (process.exitValue() != 0) {
      logger.warn("$processName 执行失败，退出码: ${process.exitValue()}")
      logger.warn("$processName 输出: $output")
      return null
    }

    return output.toString()
  } catch (e: Exception) {
    logger.warn("执行 $processName 失败", e.message)
    return null
  }
}
