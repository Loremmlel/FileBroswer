package tenshi.hinanawi.filebrowser.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.viewmodel.ImageLoadState
import tenshi.hinanawi.filebrowser.viewmodel.RemoteImageViewModel

@Composable
fun RemoteImage(
  path: String,
  viewModel: RemoteImageViewModel,
  modifier: Modifier = Modifier,
  contentDescription: String = "远程图片资源"
) {
  val imageLoadState by viewModel.image.collectAsState()
  LaunchedEffect(path) {
    viewModel.loadImage(path)
  }
  Box(modifier) {
    when (val currentState = imageLoadState) {
      is ImageLoadState.Loading ->
        CircularProgressIndicator(
          modifier = modifier
            .size(60.dp)
            .align(Alignment.Center)
        )

      is ImageLoadState.Progress ->
        LinearProgressIndicator(
          progress = { currentState.value },
          Modifier.fillMaxWidth()
        )

      is ImageLoadState.Error -> Unit

      is ImageLoadState.Success ->
        Image(
          painter = currentState.painter,
          contentDescription = contentDescription,
          modifier = modifier.fillMaxSize(),
          contentScale = ContentScale.Fit
        )
    }
  }
}