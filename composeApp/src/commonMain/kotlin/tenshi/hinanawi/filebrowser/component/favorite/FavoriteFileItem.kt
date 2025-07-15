package tenshi.hinanawi.filebrowser.component.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.component.yuzu.Thumbnail
import tenshi.hinanawi.filebrowser.data.repo.ThumbnailRepository
import tenshi.hinanawi.filebrowser.model.FileInfo

@Composable
fun FavoriteFileItem(
  modifier: Modifier = Modifier,
  file: FileInfo,
  thumbnailRepository: ThumbnailRepository,
  onClick: () -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .aspectRatio(1.5f)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .clickable { onClick() }
      .padding(vertical = 4.dp, horizontal = 8.dp),
    verticalArrangement = Arrangement.SpaceAround,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Thumbnail(
      modifier = Modifier.size(48.dp),
      file = file,
      thumbnailRepository = thumbnailRepository
    )
    Text(
      text = file.name,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
