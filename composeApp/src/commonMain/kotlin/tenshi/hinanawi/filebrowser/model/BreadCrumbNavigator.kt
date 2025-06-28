package tenshi.hinanawi.filebrowser.model

import androidx.compose.runtime.toMutableStateList
import kotlin.jvm.JvmInline

@JvmInline
value class BreadCrumbItem(
    val dirName: String
)

class BreadCrumbNavigator(
    initialPath: List<BreadCrumbItem> = emptyList(),
    private val onPathChanged: () -> Unit = {}
) {
    private val _path = initialPath.toMutableStateList()

    val path: List<BreadCrumbItem> get() = _path

    val currentDirName get() = _path.last().dirName

    val requestPath get() = "/" + _path.joinToString("/") { it.dirName }

    fun navigateTo(item: BreadCrumbItem, mergeDuplicates: Boolean = true) {
        if (mergeDuplicates) {
            val existingIndex = _path.indexOfFirst { it.dirName == item.dirName }
            if (existingIndex != -1) {
                _path.removeAll { it.dirName == item.dirName }
            }
        }
        _path.add(item)
        onPathChanged()
    }

    fun popTo(targetDirName: String, inclusive: Boolean = true) {
        val targetIndex = _path.indexOfFirst { it.dirName == targetDirName }
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