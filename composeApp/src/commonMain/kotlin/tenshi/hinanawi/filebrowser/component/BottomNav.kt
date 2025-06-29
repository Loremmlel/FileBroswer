package tenshi.hinanawi.filebrowser.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import tenshi.hinanawi.filebrowser.contant.Route

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