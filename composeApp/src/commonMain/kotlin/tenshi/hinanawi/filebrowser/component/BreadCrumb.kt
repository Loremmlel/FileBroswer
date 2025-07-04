package tenshi.hinanawi.filebrowser.component

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.model.BreadCrumbItem
import tenshi.hinanawi.filebrowser.model.BreadCrumbNavigator
import tenshi.hinanawi.filebrowser.model.ROOT_DIR_NAME
import tenshi.hinanawi.filebrowser.util.truncate


@Composable
fun BreadCrumb(
  navigator: BreadCrumbNavigator,
  modifier: Modifier = Modifier
) {
  FlowRow(
    modifier = modifier,
    itemVerticalAlignment = Alignment.CenterVertically
  ) {
    val paths = buildList {
      add(BreadCrumbItem(ROOT_DIR_NAME))
      addAll(navigator.path)
    }
    paths.forEachIndexed { index, path ->
      if (index > 0) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = "分隔符",
          modifier = Modifier
            .size(24.dp)
            .padding(horizontal = 2.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // 路径项
      TextButton(
        onClick = { navigator.popTo(path.id, false) },
        modifier = Modifier.padding(0.dp),
        contentPadding = PaddingValues(2.dp)
      ) {
        Text(
          text = path.dirName.truncate(20),
          style = MaterialTheme.typography.bodyMedium,
          color = if (index == paths.size - 1) MaterialTheme.colorScheme.onSurface
          else MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}