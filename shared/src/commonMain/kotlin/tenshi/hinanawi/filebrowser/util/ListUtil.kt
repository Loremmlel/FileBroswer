package tenshi.hinanawi.filebrowser.util

fun <T> List<T>.firstBefore(index: Int, predicate: (T) -> Boolean): T? {
  if (index !in indices) return null
  return (index - 1 downTo 0).firstOrNull { predicate(this[it]) }?.let { this[it] }
}


fun <T> List<T>.firstAfter(index: Int, predicate: (T) -> Boolean): T? {
  if (index !in indices) return null
  return (index + 1 until size).firstOrNull { predicate(this[it]) }?.let { this[it] }
}