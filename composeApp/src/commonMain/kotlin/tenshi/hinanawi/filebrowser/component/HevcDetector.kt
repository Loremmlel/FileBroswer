package tenshi.hinanawi.filebrowser.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tenshi.hinanawi.filebrowser.platform.createHevcSupportDetector

@Composable
fun HevcDetector(
  modifier: Modifier = Modifier
) {
  val hevcSupportDetector = remember { createHevcSupportDetector() }

  var expanded by remember { mutableStateOf(false) }
  var supportHevc by remember { mutableStateOf<Boolean?>(null) }

  LaunchedEffect(Unit) {
    supportHevc = hevcSupportDetector.isHevcSupported()
  }

  val (backgroundColor, contentColor) = when(supportHevc) {
    null -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    true -> Color(0xffe6f4ea) to MaterialTheme.colorScheme.primary
    false -> Color(0xfffff8e1) to MaterialTheme.colorScheme.error
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize(),
    shape = MaterialTheme.shapes.medium,
    color = backgroundColor,
    contentColor = contentColor
  ) {
    Column {
      // 顶部栏
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        if (supportHevc == null) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp).padding(end = 8.dp)
          )
          Text(
            text = "正在检测HEVC支持……",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
          )
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (supportHevc == true) Icons.Default.CheckCircle else Icons.Default.Info,
              contentDescription = if (supportHevc == true) "HEVC支持" else "HEVC不支持",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = if (supportHevc == true) "你的设备支持HEVC原生播放" else "你的设备不支持HEVC原生播放",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
          }
          IconButton(onClick = { expanded = !expanded }) {
            Icon(
              imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = if (expanded) "收起" else "HEVC检测详情"
            )
          }
        }
      }
      // 详情区域
      AnimatedVisibility(visible = expanded && supportHevc != null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
        ) {
          Text(
            text = "详细信息 ${hevcSupportDetector.detail}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          if (supportHevc == false) {
            Text(
              text = "启用HEVC支持的步骤: ",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(vertical = 4.dp)
            )
            hevcSupportDetector.solution?.let {
              SolutionList(
                items = it,
                contentColor = contentColor
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SolutionList(
  items: List<String>,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Column(modifier = modifier) {
    items.forEach { text ->
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor
      )
    }
  }
}