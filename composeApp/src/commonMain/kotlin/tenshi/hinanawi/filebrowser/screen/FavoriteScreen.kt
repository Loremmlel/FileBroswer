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
import tenshi.hinanawi.filebrowser.component.favorite.CreateFavoriteDialog
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteHeader
import tenshi.hinanawi.filebrowser.component.yuzu.BreadCrumb
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: FavoriteViewModel
) {
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
      CreateFavoriteDialog(
        onDismiss = ::onCreateDialogDismiss,
        onConfirm = { name, sortOrder ->
          onCreateDialogDismiss()
          viewModel.createFavorite(name, sortOrder)
        }
      )
    }
  }
}
