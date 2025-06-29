package tenshi.hinanawi.filebrowser.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import tenshi.hinanawi.filebrowser.exception.ApiException
import tenshi.hinanawi.filebrowser.model.ErrorInfo

object ErrorHandler {
  private val _errorFlow = MutableSharedFlow<ErrorInfo>()
  val errorFlow: SharedFlow<ErrorInfo> = _errorFlow

  suspend fun showError(code: Int, message: String) {
    _errorFlow.emit(ErrorInfo(code, message))
  }

  suspend fun handleException(exception: Throwable) {
    val (code, message) = when (exception) {
      is ApiException -> exception.code to "API请求错误: ${exception.code}: ${exception.message}"
      else -> 0 to "未知错误: ${exception.message}"
    }
    showError(code, message)
  }
}