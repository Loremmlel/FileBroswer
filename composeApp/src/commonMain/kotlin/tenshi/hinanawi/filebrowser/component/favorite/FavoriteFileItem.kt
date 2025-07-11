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
import tenshi.hinanawi.filebrowser.component.yuzu.FileTypeIcon
import tenshi.hinanawi.filebrowser.model.FileInfo

@Composable
fun FavoriteFileItem(
  modifier: Modifier = Modifier,
  file: FileInfo,
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
    FileTypeIcon(
      modifier = Modifier,
      fileType = file.type,
      iconSize = 32.dp
    )
    Text(
      text = file.name,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
