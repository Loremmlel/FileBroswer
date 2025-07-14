package tenshi.hinanawi.filebrowser.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharedFlow
import tenshi.hinanawi.filebrowser.component.favorite.CreateFavoriteModal
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteHeader
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteItem
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: FavoriteViewModel,
  appNavController: NavController
) {
  val uiState by viewModel.uiState.collectAsState()

  var createDialogVisible by remember { mutableStateOf(false) }

  fun onCreateClick() {
    createDialogVisible = true
  }

  fun onCreateDialogDismiss() {
    createDialogVisible = false
  }

  EventHandler(event = viewModel.event)

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
      if (!uiState.loading) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          items(items = uiState.favorites, key = { it.id }) { favorite ->
            FavoriteItem(
              modifier = Modifier
                .fillMaxWidth(),
              favorite = favorite,
              onClick = {},
              onItemClick = { fileInfo ->
                appNavController.previousBackStackEntry?.savedStateHandle?.apply {
                  set("path", fileInfo.path)
                  set("previewItemName", fileInfo.name)
                }
                appNavController.popBackStack()
              }
            )
          }
        }
      }
    }
    if (uiState.loading) {
      CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp))
    }
    if (createDialogVisible) {
      CreateFavoriteModal(
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

@Composable
private fun EventHandler(event: SharedFlow<FavoriteViewModel.Event>) {
  LaunchedEffect(Unit) {
    event.collect {
      when (it) {
        is FavoriteViewModel.Event.CreateSuccess -> {
          Toast.makeText("收藏夹创建成功", Toast.LONG).show()
        }
      }
    }
  }
}