package tenshi.hinanawi.filebrowser.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.model.FileInfo
import tenshi.hinanawi.filebrowser.model.FileType
import tenshi.hinanawi.filebrowser.util.formatFileSize

@Composable
internal fun FileItem(
  file: FileInfo,
  onClick: (FileInfo) -> Unit,
  onDelete: ((FileInfo) -> Unit)? = null,
  onDownload: ((FileInfo) -> Unit)? = null,
  isFavorite: Boolean = false,
  onFavoriteToggle: (Boolean) -> Unit = {},
) {
  val iconButtonSize = 18.dp
  val iconButtonPadding = 4.dp
  val iconSize = 48.dp
  var showConfirm by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .clickable { onClick(file) }
      .padding(12.dp)
  ) {
    // 顶部操作按钮
    Row(
      modifier = Modifier.align(Alignment.TopEnd),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      IconButton(
        onClick = { onFavoriteToggle(isFavorite) },
        modifier = Modifier
          .background(
            color = if (isFavorite) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
          ).size(iconButtonSize)
          .padding(iconButtonPadding)
      ) {
        Icon(
          imageVector = if (isFavorite) Icons.Default.Favorite
          else Icons.Default.FavoriteBorder,
          contentDescription = if (isFavorite) "取消收藏"
          else "收藏",
          tint = if (isFavorite) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // 下载按钮（仅对图片/视频）
      if (onDownload != null && (file.type == FileType.Image || file.type == FileType.Video)) {
        IconButton(
          onClick = { onDownload(file) },
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape
            )
            .size(iconButtonSize)
            .padding(iconButtonPadding)
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "下载",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (onDelete != null) {
        IconButton(
          onClick = { showConfirm = true },
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = CircleShape
            )
            .size(iconButtonSize)
            .padding(iconButtonPadding)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "删除",
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }
    // 文件内容区域
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp)
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      when (file.type) {
        FileType.Folder -> Icon(
          imageVector = Icons.Default.Folder,
          contentDescription = "文件夹",
          modifier = Modifier.size(iconSize),
          tint = MaterialTheme.colorScheme.primary
        )

        FileType.Image -> Icon(
          imageVector = Icons.Default.Image,
          contentDescription = "图片",
          modifier = Modifier.size(iconSize),
          tint = MaterialTheme.colorScheme.primary
        )

        FileType.Video -> Icon(
          imageVector = Icons.Default.VideoFile,
          contentDescription = "视频",
          modifier = Modifier.size(iconSize),
          tint = MaterialTheme.colorScheme.primary
        )

        FileType.Other -> Icon(
          imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
          contentDescription = "其他",
          modifier = Modifier.size(iconSize),
          tint = MaterialTheme.colorScheme.primary
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      // 文件名
      Text(
        text = file.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      // 文件大小
      if (!file.isDirectory) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = file.size.formatFileSize(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // 删除确认对话
    if (showConfirm) {
      AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("确认删除") },
        text = { Text("确定要删除 ${file.name} 吗？") },
        confirmButton = {
          Button(
            onClick = {
              showConfirm = false
              onDelete?.invoke(file)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer
            )
          ) {
            Text("删除")
          }
        },
        dismissButton = {
          TextButton(onClick = { showConfirm = false }) {
            Text("取消")
          }
        },
        modifier = Modifier.background(
          MaterialTheme.colorScheme.surface,
          RoundedCornerShape(12.dp)
        )
      )
    }
  }
}