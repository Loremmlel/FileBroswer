package tenshi.hinanawi.filebrowser

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import tenshi.hinanawi.filebrowser.component.yuzu.BottomNav
import tenshi.hinanawi.filebrowser.component.yuzu.ErrorOverlay
import tenshi.hinanawi.filebrowser.contant.Route
import tenshi.hinanawi.filebrowser.data.online.OnlineFavoriteRepository
import tenshi.hinanawi.filebrowser.data.online.OnlineFileRepository
import tenshi.hinanawi.filebrowser.screen.BrowseScreen
import tenshi.hinanawi.filebrowser.screen.FavoriteScreen
import tenshi.hinanawi.filebrowser.viewmodel.BrowseViewModel
import tenshi.hinanawi.filebrowser.viewmodel.FavoriteViewModel

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
  // 不知道为什么cmp不支持viewModel函数，然后传入factory参数来构造viewModel
  // 只好采用土法了
  // uiState的异常也和这个有关。原本我是在函数参数的默认值构造viewModel的，可能在重新组合的时候造成一些问题
  val browseViewModel = remember { BrowseViewModel(OnlineFileRepository()) }
  val favoriteViewModel = remember { FavoriteViewModel(OnlineFavoriteRepository()) }
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
            BrowseScreen(viewModel = browseViewModel)
          }
          composable(route = Route.FavoriteScreen.stringRoute) {
            FavoriteScreen(viewModel = favoriteViewModel)
          }
        }
        BottomNav(
          modifier = Modifier.fillMaxWidth().weight(1f),
          navController
        )
      }
      ErrorOverlay(modifier = Modifier.fillMaxSize())
    }
  }
}