package tenshi.hinanawi.filebrowser.viewmodel

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
  private val _event = MutableSharedFlow<Event>(
    replay = 0,
    extraBufferCapacity = 16,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val event = _event.asSharedFlow()

  sealed class Event {
    object NotifyFavoriteFileAdd : Event()
    object NotifyFavoriteFileRemove : Event()
  }

  suspend fun emit(event: Event) {
    _event.emit(event)
  }
}