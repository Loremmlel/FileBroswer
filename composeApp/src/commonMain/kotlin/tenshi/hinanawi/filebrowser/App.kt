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
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import tenshi.hinanawi.filebrowser.component.yuzu.BottomNav
import tenshi.hinanawi.filebrowser.component.yuzu.ErrorOverlay
import tenshi.hinanawi.filebrowser.component.yuzu.ToastContainer
import tenshi.hinanawi.filebrowser.constant.Route
import tenshi.hinanawi.filebrowser.data.repo.OnlineFavoriteRepository
import tenshi.hinanawi.filebrowser.data.repo.OnlineFileRepository
import tenshi.hinanawi.filebrowser.screen.BrowseScreen
import tenshi.hinanawi.filebrowser.screen.FavoriteScreen
import tenshi.hinanawi.filebrowser.util.slideComposable
import tenshi.hinanawi.filebrowser.util.toBreadCrumbItem
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
  val browseViewModel = remember {
    BrowseViewModel(
      filesRepository = OnlineFileRepository(),
      favoriteRepository = OnlineFavoriteRepository()
    )
  }
  val favoriteViewModel = remember { FavoriteViewModel(OnlineFavoriteRepository()) }
  MaterialTheme {
    ToastContainer {
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
            slideComposable(
              route = Route.MainScreen.stringRoute
            ) { navBackStackEntry ->
              val pathString: String = navBackStackEntry.savedStateHandle["path"] ?: "/"
              val path = remember(pathString) {
                pathString.split("/").toMutableList().apply {
                  // 去掉开头/造成的空字符串元素
                  removeFirst()
                  // 去掉末尾的文件名元素
                  removeLast()
                }.map { it.toBreadCrumbItem() }
              }
              val previewItemName: String? = navBackStackEntry.savedStateHandle["previewItemName"]
              BrowseScreen(
                viewModel = browseViewModel,
                path = path,
                previewItemName = previewItemName
              )
            }
            slideComposable(
              route = Route.FavoriteScreen.stringRoute
            ) {
              FavoriteScreen(
                viewModel = favoriteViewModel,
                appNavController = navController
              )
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
}