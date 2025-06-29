package tenshi.hinanawi.filebrowser.exception

import tenshi.hinanawi.filebrowser.model.Message

class ApiException(
  val code: Int,
  message: Message
) : Exception(message.message)