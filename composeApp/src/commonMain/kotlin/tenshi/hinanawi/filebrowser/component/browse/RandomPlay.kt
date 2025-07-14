package tenshi.hinanawi.filebrowser.component.browse

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.viewmodel.RandomPlayViewModel

@Composable
fun RandomPlay(
  modifier: Modifier = Modifier,
  viewModel: RandomPlayViewModel,
  currentPath: String,
  onVideoPlay: (FileInfo) -> Unit
) {
  val state by viewModel.uiState.collectAsState()

  LaunchedEffect(currentPath) {
    viewModel.pathChange()
  }

  Button(
    onClick = {
      if (state.videoFiles.isEmpty() && !state.loading) {
        viewModel.getAllVideos(currentPath)
      } else {
        val video = viewModel.getRandomVideo()
        video?.let {
          onVideoPlay(it)
        }
      }
    },
    modifier = modifier
      .wrapContentHeight()
  ) {
    Text(
      text = when {
        state.loading -> "加载中..."
        state.videoFiles.isEmpty() -> "无视频文件, 点击加载"
        else -> "随机播放"
      }
    )
  }
}