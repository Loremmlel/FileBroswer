package tenshi.hinanawi.filebrowser.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tenshi.hinanawi.filebrowser.model.FileInfo

@Composable
fun ImageViewer(
  file: FileInfo,
  onDismiss: () -> Unit,
  onPrev: (() -> Unit)? = null,
  onNext: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val operationIconSize = 32.dp
  val iconButtonPadding = 4.dp
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.9f)),
    contentAlignment = Alignment.Center
  ) {
    RemoteImage(
      path = file.path,
      contentDescription = file.name,
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 60.dp, horizontal = 40.dp)
    )
    // 标题栏
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = file.name,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // 操作按钮
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      contentAlignment = Alignment.TopEnd
    ) {
      Row {
        // 下载按钮
        IconButton(
          onClick = { },
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape
            )
            .size(operationIconSize)
            .padding(iconButtonPadding)
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "下载",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = CircleShape
            )
            .size(operationIconSize)
            .padding(iconButtonPadding)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "关闭图片预览",
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }

    // 导航按钮 - 上一张
    if (onPrev != null) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 16.dp)
      ) {
        NavigationButton(
          icon = Icons.AutoMirrored.Default.ArrowBack,
          onClick = onPrev
        )
      }
    }

    // 导航按钮 - 下一张
    if (onNext != null) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 16.dp)
      ) {
        NavigationButton(
          icon = Icons.AutoMirrored.Default.ArrowForward,
          onClick = onNext
        )
      }
    }
  }
}

@Composable
private fun NavigationButton(
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(Color.Black.copy(alpha = 0.5f))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "导航",
      tint = Color.White,
      modifier = Modifier.size(32.dp)
    )
  }
}