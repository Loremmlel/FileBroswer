package tenshi.hinanawi.filebrowser.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.yuzu.BreadCrumb
import tenshi.hinanawi.filebrowser.component.yuzu.ImageViewer
import tenshi.hinanawi.filebrowser.component.browse.FileItem
import tenshi.hinanawi.filebrowser.component.browse.HevcDetector
import tenshi.hinanawi.filebrowser.component.browse.RandomPlay
import tenshi.hinanawi.filebrowser.data.online.OnlineRandomRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbItem
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.viewmodel.BrowseViewModel
import tenshi.hinanawi.filebrowser.viewmodel.RandomPlayViewModel


@Composable
fun BrowseScreen(
  modifier: Modifier = Modifier,
  viewModel: BrowseViewModel
) {
  val state by viewModel.uiState.collectAsState()
  val randomPlayViewModel = remember { RandomPlayViewModel(OnlineRandomRepository()) }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    BreadCrumb(
      navigator = viewModel.navigator,
      modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HevcDetector()
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth()
    ) {
      when {
        state.fileLoading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("加载中...", style = MaterialTheme.typography.bodyLarge)
          }
        }

        state.files.isEmpty() -> {
          Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
          ) {
            Text("文件夹为空", style = MaterialTheme.typography.bodyLarge)
          }
        }

        else -> {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            items(state.files, key = { file -> file.name }) { file ->
              FileItem(
                file = file,
                onClick = {
                  when {
                    file.isDirectory -> viewModel.navigator.navigateTo(BreadCrumbItem(it.name, it.name))
                    file.type == FileType.Image -> viewModel.openImagePreview(it)
                  }
                },
                onDelete = {
                  viewModel.deleteFile(it)
                },
                onDownload = if (!file.isDirectory) { fileInfo ->
                  viewModel.downloadFile(fileInfo)
                } else null,
                isFavorite = false,
                onFavoriteToggle = {

                })
            }
          }
        }
      }
      RandomPlay(
        modifier = Modifier.align(Alignment.BottomCenter),
        viewModel = randomPlayViewModel,
        currentPath = viewModel.navigator.requestPath,
        onVideoPlay = viewModel::playVideo
      )
    }
    state.previewItem?.let { previewItem ->
      ImageViewer(
        file = previewItem,
        onDismiss = viewModel::closeImagePreview,
        onNext = viewModel::nextImagePreview,
        onPrev = viewModel::previousImagePreview,
        onDownload = viewModel::downloadFile
      )
    }
  }
}