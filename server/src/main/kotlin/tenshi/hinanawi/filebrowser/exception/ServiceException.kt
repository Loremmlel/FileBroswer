package tenshi.hinanawi.filebrowser.exception

import tenshi.hinanawi.filebrowser.service.ServiceMessage

class ServiceException(val serviceMessage: ServiceMessage) : Exception(serviceMessage.message)