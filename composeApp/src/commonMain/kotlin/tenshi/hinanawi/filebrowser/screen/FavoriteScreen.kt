package tenshi.hinanawi.filebrowser.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.yuzu.BreadCrumb
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteHeader
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: FavoriteViewModel
) {
  val textFieldPadding = 16.dp

  val state by viewModel.uiState.collectAsState()
  var createDialogVisible by remember { mutableStateOf(false) }

  fun onCreateClick() {
    createDialogVisible = true
  }

  fun onCreateDialogDismiss() {
    createDialogVisible = false
  }
  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      FavoriteHeader(
        onTreeClick = {},
        onAddClick = ::onCreateClick,
        onDeleteClick = {}
      )
      BreadCrumb(
        navigator = viewModel.navigator,
        modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp, vertical = 8.dp)
      )
    }
    if (createDialogVisible) {
      var createDialogName by remember { mutableStateOf("") }
      var createDialogSortOrder by remember { mutableIntStateOf(0) }
      AlertDialog(
        onDismissRequest = ::onCreateDialogDismiss,
        modifier = Modifier.align(Alignment.Center),
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
          TextButton(onClick = {
            onCreateDialogDismiss()
            viewModel.createFavorite(createDialogName, createDialogSortOrder)
          }) {
            Text(text = "添加")
          }
        },
        dismissButton = {
          TextButton(onClick = ::onCreateDialogDismiss) {
            Text(text = "取消")
          }
        }
      )
    }
  }
}
