package tenshi.hinanawi.filebrowser.component.favorite

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateFavoriteModal(
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
  onConfirm: (String, Int) -> Unit
) {
  val textFieldPadding = 16.dp

  var createDialogName by remember { mutableStateOf("") }
  var createDialogSortOrder by remember { mutableIntStateOf(0) }
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "在当前目录添加收藏夹",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = textFieldPadding)
        )
        TextField(
          value = createDialogName,
          onValueChange = { createDialogName = it },
          label = { Text(text = "收藏夹名称") },
          modifier = Modifier.fillMaxWidth().padding(vertical = textFieldPadding)
        )
        TextField(
          value = createDialogSortOrder.toString(),
          onValueChange = { createDialogSortOrder = it.toIntOrNull() ?: createDialogSortOrder },
          label = { Text(text = "排序") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().padding(vertical = textFieldPadding)
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(createDialogName, createDialogSortOrder) }) {
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