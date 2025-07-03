package tenshi.hinanawi.filebrowser.exception

import tenshi.hinanawi.filebrowser.schema.ServiceMessage

class ServiceException(message: ServiceMessage) : Exception(message.message)