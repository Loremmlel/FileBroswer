package tenshi.hinanawi.filebrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import tenshi.hinanawi.filebrowser.component.BottomNav
import tenshi.hinanawi.filebrowser.component.ErrorOverlay
import tenshi.hinanawi.filebrowser.contant.Route

@Composable
@Preview
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val mainScreenRatio = 0.88f
    val navController = rememberNavController()
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavHost(
                    modifier = Modifier.fillMaxHeight(mainScreenRatio),
                    navController = navController,
                    startDestination = Route.MainScreen.stringRoute
                ) {
                    composable(route = Route.MainScreen.stringRoute) {
                        Text("我是主页")
                    }
                    composable(route = Route.FavoriteScreen.stringRoute) {
                        Text("我是收藏")
                    }
                }
                BottomNav(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    navController
                )
            }
            ErrorOverlay()
        }
    }
}