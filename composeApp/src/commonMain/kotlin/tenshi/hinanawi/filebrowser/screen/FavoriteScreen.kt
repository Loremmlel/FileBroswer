package tenshi.hinanawi.filebrowser.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.BreadCrumb
import tenshi.hinanawi.filebrowser.component.favorite.FavoriteHeader
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

@Composable
fun FavoriteScreen(
  modifier: Modifier = Modifier,
  viewModel: FavoriteViewModel
) {
  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    FavoriteHeader(
      onTreeClick = {},
      onAddClick = {},
      onDeleteClick = {}
    )
    BreadCrumb(
      navigator = viewModel.navigator,
      modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp, vertical = 8.dp)
    )
  }
}
