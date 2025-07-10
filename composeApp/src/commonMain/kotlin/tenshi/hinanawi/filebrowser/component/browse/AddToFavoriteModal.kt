package tenshi.hinanawi.filebrowser.component.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.model.FavoriteDto

@Composable
fun AddToFavoritesModal(
  modifier: Modifier = Modifier,
  favorites: List<FavoriteDto>,
  onDismiss: () -> Unit,
  onAdd: (Long) -> Unit
) {
  var selectedFavoriteId by remember { mutableStateOf<Long?>(null) }
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        items(favorites) { favorite ->
          FavoriteCard(
            favorite = favorite,
            selected = selectedFavoriteId == favorite.id,
            onClick = { selectedFavoriteId = favorite.id }
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = selectedFavoriteId != null,
        onClick = {
          selectedFavoriteId?.let { onAdd(it) } ?: Toast.makeText("喵喵BUG，这可能出现吗?", Toast.LONG).show()
        }
      ) {
        Text(text = "添加")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = "取消")
      }
    }
  )
}

@Composable
private fun FavoriteCard(
  favorite: FavoriteDto,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
      )
      .clickable { onClick() }
      .clip(RoundedCornerShape(16.dp))
      .padding(8.dp)
  ) {
    Icon(
      modifier = Modifier.align(Alignment.Center),
      imageVector = if (selected) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial,
      contentDescription = "收藏夹${favorite.name}",
      tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    )
    Text(
      text = favorite.name,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(start = 8.dp)
    )
  }
}