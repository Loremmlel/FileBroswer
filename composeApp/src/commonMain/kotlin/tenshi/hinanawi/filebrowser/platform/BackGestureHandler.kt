package tenshi.hinanawi.filebrowser.platform

import androidx.compose.runtime.Composable

@Composable
expect fun BackGestureHandler(enabled: Boolean = true, onBack: () -> Unit)