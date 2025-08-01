package tenshi.hinanawi.filebrowser.component.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.data.repo.OnlineThumbnailRepository
import tenshi.hinanawi.filebrowser.model.dto.FavoriteDto
import tenshi.hinanawi.filebrowser.model.dto.toFileInfo
import tenshi.hinanawi.filebrowser.model.response.FileInfo

@Composable
fun FavoriteItem(
  modifier: Modifier = Modifier,
  favorite: FavoriteDto,
  onClick: () -> Unit,
  onItemClick: (FileInfo) -> Unit
) {
  val thumbnailRepository = remember { OnlineThumbnailRepository() }
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
      )
      .padding(vertical = 4.dp, horizontal = 12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = favorite.name,
        style = MaterialTheme.typography.titleLarge
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "${favorite.files.size}个内容",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
          modifier = Modifier.size(16.dp),
          imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
          contentDescription = "进入收藏夹${favorite.name}"
        )
      }
    }
    LazyRow(
      modifier = Modifier.fillMaxWidth().height(120.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      contentPadding = PaddingValues(4.dp)
    ) {
      if (favorite.files.isEmpty()) {
        item {
          Text(
            text = "收藏夹为空",
            style = MaterialTheme.typography.bodyLarge
          )
        }
        return@LazyRow
      }
      items(favorite.files) { file ->
        FavoriteFileItem(
          file = file.toFileInfo(),
          thumbnailRepository = thumbnailRepository,
          onClick = { onItemClick(file.toFileInfo()) }
        )
      }
    }
  }
}