package tenshi.hinanawi.filebrowser.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tenshi.hinanawi.filebrowser.model.ErrorInfo
import tenshi.hinanawi.filebrowser.util.ErrorHandler

@Composable
internal fun ErrorOverlay(modifier: Modifier = Modifier) {
    var currentError by remember { mutableStateOf<ErrorInfo?>(null) }

    LaunchedEffect(Unit) {
        ErrorHandler.errorFlow.collect { error ->
            currentError = error
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = currentError != null
    ) {
        currentError?.let { error ->
            ErrorOverlayContent(error, { currentError = null })
        }
    }
}

@Composable
private fun ErrorOverlayContent(
    error: ErrorInfo,
    onDismiss: () -> Unit
) {
    val (backgroundColor, contentColor, icon) = getErrorStyle(error.code)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .clickable {},  // 阻止点击穿透
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 错误图标
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                // 错误标题
                Text(
                    text = getErrorTitle(error.code),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 错误消息
                Text(
                    text = error.message,
                    fontSize = 16.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                // 确定按钮
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = backgroundColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "确定",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getErrorStyle(code: Int): Triple<Color, Color, ImageVector> = when (code) {
    in 400..499 -> Triple(
        Color(0xFFFFF3CD),
        Color(0xFF856404),
        Icons.Default.Warning
    )

    in 500..599 -> Triple(
        Color(0xFFF8D7DA),
        Color(0xFF721C24),
        Icons.Default.Error
    )

    else -> Triple(
        Color(0xFFE2E3E5),
        Color(0xFF383D41),
        Icons.Default.Error
    )
}

private fun getErrorTitle(code: Int): String = when (code) {
    in 400..499 -> "客户端错误"
    in 500..599 -> "服务器错误"
    else -> "系统错误"
}