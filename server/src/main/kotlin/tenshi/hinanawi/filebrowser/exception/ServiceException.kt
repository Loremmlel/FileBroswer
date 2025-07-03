package tenshi.hinanawi.filebrowser.exception

import tenshi.hinanawi.filebrowser.schema.ServiceMessage

class ServiceException(val serviceMessage: ServiceMessage) : Exception(serviceMessage.message)