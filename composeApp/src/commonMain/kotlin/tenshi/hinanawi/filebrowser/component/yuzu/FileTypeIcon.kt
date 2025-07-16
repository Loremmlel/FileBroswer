package tenshi.hinanawi.filebrowser.component.yuzu

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.model.response.FileType

@Composable
fun FileTypeIcon(
  modifier: Modifier = Modifier,
  fileType: FileType,
  iconSize: Dp = 48.dp
) {
  when (fileType) {
    FileType.Folder -> Icon(
      imageVector = Icons.Default.Folder,
      contentDescription = "文件夹",
      modifier = modifier.size(iconSize),
      tint = MaterialTheme.colorScheme.primary
    )

    FileType.Image -> Icon(
      imageVector = Icons.Default.Image,
      contentDescription = "图片",
      modifier = modifier.size(iconSize),
      tint = MaterialTheme.colorScheme.primary
    )

    FileType.Video -> Icon(
      imageVector = Icons.Default.VideoFile,
      contentDescription = "视频",
      modifier = modifier.size(iconSize),
      tint = MaterialTheme.colorScheme.primary
    )

    FileType.Other -> Icon(
      imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
      contentDescription = "其他",
      modifier = modifier.size(iconSize),
      tint = MaterialTheme.colorScheme.primary
    )
  }
}
