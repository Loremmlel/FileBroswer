package tenshi.hinanawi.filebrowser.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

fun Modifier.nullIndicatorClickable(
  enabled: Boolean = true,
  onClick: () -> Unit
) = this.clickable(
  interactionSource = MutableInteractionSource(),
  indication = null,
  enabled = enabled,
  onClick = onClick
)