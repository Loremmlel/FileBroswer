package tenshi.hinanawi.filebrowser.component.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.model.FavoriteDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToFavoritesModal(
  modifier: Modifier = Modifier,
  favorites: List<FavoriteDto>,
  onDismiss: () -> Unit,
  onAdd: (Long) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  var selectedFavorite by remember { mutableStateOf<FavoriteDto?>(null) }
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    text = {
      ExposedDropdownMenuBox(
        modifier = Modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it }
      ) {
        TextField(
          modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
          readOnly = true,
          value = selectedFavorite?.name ?: "选择收藏夹",
          onValueChange = { },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          favorites.forEach { favorite ->
            DropdownMenuItem(
              text = { Text(text = favorite.name) },
              onClick = {
                selectedFavorite = favorite
                expanded = false
              }
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = selectedFavorite != null,
        onClick = {
          selectedFavorite?.let { onAdd(it.id) } ?: Toast.makeText("喵喵BUG，这可能出现吗?", Toast.LONG).show()
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