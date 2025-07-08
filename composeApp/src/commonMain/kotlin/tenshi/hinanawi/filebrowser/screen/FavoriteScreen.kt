package tenshi.hinanawi.filebrowser.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.favorite.CreateFavoriteDialog
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteHeader
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: FavoriteViewModel
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.events.collect {
      when (it) {
        is FavoriteViewModel.Event.CreateSuccess -> {
          Toast.makeText("收藏夹创建成功", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

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
    }
    if (uiState.loading) {
      CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp))
    } else {

    }
    if (createDialogVisible) {
      CreateFavoriteDialog(
        modifier = Modifier.align(Alignment.Center),
        onDismiss = ::onCreateDialogDismiss,
        onConfirm = { name, sortOrder ->
          onCreateDialogDismiss()
          viewModel.createFavorite(name, sortOrder)
        }
      )
    }
  }
}
