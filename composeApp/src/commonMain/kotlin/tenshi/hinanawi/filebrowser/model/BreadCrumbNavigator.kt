package tenshi.hinanawi.filebrowser.model

import androidx.compose.runtime.toMutableStateList

internal const val ROOT_DIR_NAME = "/"
internal val ROOT_ID: String? = null

class BreadCrumbItem(
  val dirName: String,
  val id: String? = null
)

class BreadCrumbNavigator(
  initialPath: List<BreadCrumbItem> = emptyList(),
  private val onPathChanged: () -> Unit = {}
) {
  private val _path = initialPath.toMutableStateList()

  val path: List<BreadCrumbItem> get() = _path

  val currentId get() = _path.lastOrNull()?.id

  val requestPath get() = "/" + _path.joinToString("/") { it.dirName }

  fun navigateTo(item: BreadCrumbItem, mergeDuplicates: Boolean = true) {
    if (mergeDuplicates) {
      val existingIndex = _path.indexOfFirst { it.id == item.id }
      if (existingIndex != -1) {
        _path.removeAll { it.id == item.id }
      }
    }
    _path.add(item)
    onPathChanged()
  }

  fun navigateTo(items: List<BreadCrumbItem>) {
    _path.clear()
    _path.addAll(items)
    onPathChanged()
  }

  fun popTo(targetId: String?, inclusive: Boolean = true) {
    if (targetId == ROOT_ID) {
      resetToRoot()
    }
    val targetIndex = _path.indexOfFirst { it.id == targetId }
    if (targetIndex != -1) {
      val removeFrom = if (inclusive) targetIndex else targetIndex + 1
      _path.removeAll { _path.indexOf(it) >= removeFrom }
      onPathChanged()
    }
  }

  fun popBack() {
    if (_path.isNotEmpty()) {
      _path.removeLast()
      onPathChanged()
    }
  }

  fun resetToRoot() {
    _path.clear()
    onPathChanged()
  }
}

fun String.toBreadCrumbItem() = BreadCrumbItem(this, this)