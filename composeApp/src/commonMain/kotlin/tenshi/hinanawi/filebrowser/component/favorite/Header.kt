package tenshi.hinanawi.filebrowser.component.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FavoriteHeader(
  modifier: Modifier = Modifier,
  onAddClick: () -> Unit,
  onTreeClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  val iconButtonSize = 30.dp
  Row(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .background(color = MaterialTheme.colorScheme.primaryContainer)
      .padding(horizontal = 16.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    IconButton(
      modifier = Modifier
        .size(iconButtonSize),
      onClick = onTreeClick
    ) {
      Icon(
        imageVector = Icons.Default.AccountTree,
        contentDescription = "收藏夹树",
        tint = MaterialTheme.colorScheme.primary
      )
    }
    Row(
      modifier = Modifier
        .width(80.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      IconButton(
        modifier = Modifier
          .size(iconButtonSize),
        onClick = onDeleteClick
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "删除",
          tint = MaterialTheme.colorScheme.primary
        )
      }
      IconButton(
        modifier = Modifier
          .size(iconButtonSize),
        onClick = onAddClick
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "添加",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}