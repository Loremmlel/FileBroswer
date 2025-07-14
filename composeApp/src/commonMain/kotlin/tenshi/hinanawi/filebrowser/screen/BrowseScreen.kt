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
import kotlinx.coroutines.flow.SharedFlow
import tenshi.hinanawi.filebrowser.component.browse.AddToFavoritesModal
import tenshi.hinanawi.filebrowser.component.browse.FileItem
import tenshi.hinanawi.filebrowser.component.browse.HevcDetector
import tenshi.hinanawi.filebrowser.component.browse.RandomPlay
import tenshi.hinanawi.filebrowser.component.yuzu.BreadCrumb
import tenshi.hinanawi.filebrowser.component.yuzu.ImageViewer
import tenshi.hinanawi.filebrowser.component.yuzu.Toast
import tenshi.hinanawi.filebrowser.data.online.OnlineRandomRepository
import tenshi.hinanawi.filebrowser.model.BreadCrumbItem
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.util.polling
import tenshi.hinanawi.filebrowser.viewmodel.BrowseViewModel
import tenshi.hinanawi.filebrowser.viewmodel.RandomPlayViewModel


@Composable
fun BrowseScreen(
  modifier: Modifier = Modifier,
  viewModel: BrowseViewModel,
  path: List<BreadCrumbItem> = emptyList(),
  previewItemName: String? = null
) {
  val uiState by viewModel.uiState.collectAsState()
  val randomPlayViewModel = remember { RandomPlayViewModel(OnlineRandomRepository()) }

  var addToFavoriteModalVisible by remember { mutableStateOf(false) }

  LaunchedEffect(path, previewItemName) {
    viewModel.navigator.navigateTo(path)
    if (previewItemName != null) {
      polling(
        { !uiState.fileLoading }
      ) {
        val previewItem = uiState.files.firstOrNull { it.name == previewItemName } ?: return@polling
        viewModel.openPreview(previewItem)
      }
    }
  }

  EventHandler(event = viewModel.event)

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
        uiState.fileLoading -> {
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

        uiState.files.isEmpty() -> {
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
            items(uiState.files, key = { file -> file.name }) { file ->
              FileItem(
                file = file,
                onClick = {
                  when {
                    file.isDirectory -> viewModel.navigator.navigateTo(BreadCrumbItem(it.name, it.name))
                    file.type == FileType.Image -> viewModel.openPreview(it)
                  }
                },
                onDelete = {
                  viewModel.deleteFile(it)
                },
                onDownload = if (!file.isDirectory) { fileInfo ->
                  viewModel.downloadFile(fileInfo)
                } else null,
                isFavorite = uiState.favoriteFilesMap.containsKey(file.path),
                onFavoriteToggle = { isFavorite ->
                  if (isFavorite) {
                    uiState.favoriteFilesMap[file.path]?.let {
                      viewModel.cancelFavoriteFile(it)
                    }
                  } else {
                    viewModel.setCurrentFavoriteFile(file)
                    addToFavoriteModalVisible = true
                  }
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
      if (addToFavoriteModalVisible) {
        AddToFavoritesModal(
          modifier = Modifier
            .align(Alignment.Center),
          favorites = uiState.favorites,
          onDismiss = {
            viewModel.setCurrentFavoriteFile(null)
            addToFavoriteModalVisible = false
          },
          onAdd = {
            viewModel.addFileToFavorite(it)
            addToFavoriteModalVisible = false
          }
        )
      }
    }
    uiState.previewItem?.let { previewItem ->
      when (previewItem.type) {
        FileType.Image -> {
          ImageViewer(
            file = previewItem,
            onDismiss = viewModel::closePreview,
            onNext = viewModel::nextImagePreview,
            onPrev = viewModel::previousImagePreview,
            onDownload = viewModel::downloadFile
          )
        }

        else -> {}
      }
    }
  }
}

@Composable
private fun EventHandler(event: SharedFlow<BrowseViewModel.Event>) {
  LaunchedEffect(Unit) {
    event.collect {
      when (it) {
        is BrowseViewModel.Event.NoImagePreview -> Toast.makeText(
          "没有图片正在预览",
          Toast.SHORT
        ).show()

        is BrowseViewModel.Event.IsLastImage -> Toast.makeText(
          "已经是最后一张图片了, 预览第一张图片",
          Toast.VERY_SHORT
        ).show()

        is BrowseViewModel.Event.IsFirstImage -> Toast.makeText(
          "已经是第一张图片了, 预览最后一张图片",
          Toast.VERY_SHORT
        ).show()

        is BrowseViewModel.Event.AddFileToFavoriteSuccess -> Toast.makeText(
          "添加收藏夹成功",
          Toast.SHORT
        ).show()

        is BrowseViewModel.Event.TryingPreviewNull -> Toast.makeText(
          "尝试预览空文件",
          Toast.SHORT
        ).show()

        is BrowseViewModel.Event.CancelFavoriteFileSuccess -> Toast.makeText(
          "取消收藏该文件成功",
          Toast.SHORT
        ).show()
      }
    }
  }
}