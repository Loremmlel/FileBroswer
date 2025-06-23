package tenshi.hinanawi.filebrowser.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import tenshi.hinanawi.filebrowser.contant.Route
import tenshi.hinanawi.filebrowser.util.nullIndicatorClickable

@Composable
fun BottomNav(
    modifier: Modifier,
    navController: NavController
) {
    val items = listOf(
        Triple(Route.MainScreen.stringRoute, "主页", Icons.Default.Videocam),
        Triple(Route.FavoriteScreen.stringRoute, "收藏", Icons.Default.Star)
    )

    var selectedItem by remember { mutableStateOf(Route.MainScreen.stringRoute) }

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selectedItem == route) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        }
                    )
                },
                label = { Text(label) },
                selected = selectedItem == route,
                onClick = {
                    selectedItem = route
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                }
            )
        }
    }
}