package tenshi.hinanawi.filebrowser.util

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlin.jvm.JvmSuppressWildcards

const val ANIMATION_DURATION = 500

/**
 * 自己封装的NavGraphBuilder扩展函数。
 *
 * 主要封装了动画效果，默认为滑入滑出的动画。适合非栈底的界面使用。
 * @see composable
 */
fun NavGraphBuilder.slideComposable(
  route: String,
  arguments: List<NamedNavArgument> = emptyList(),
  deepLinks: List<NavDeepLink> = emptyList(),
  enterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = {
    slideInHorizontally(animationSpec = tween(ANIMATION_DURATION), initialOffsetX = { it })
  },
  exitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = {
    slideOutHorizontally(animationSpec = tween(ANIMATION_DURATION), targetOffsetX = { -it })
  },
  popEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = {
    slideInHorizontally(animationSpec = tween(ANIMATION_DURATION), initialOffsetX = { -it })
  },
  popExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = {
    slideOutHorizontally(animationSpec = tween(ANIMATION_DURATION), targetOffsetX = { it })
  },
  content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(
  route = route,
  arguments = arguments,
  deepLinks = deepLinks,
  enterTransition = enterTransition,
  exitTransition = exitTransition,
  popEnterTransition = popEnterTransition,
  popExitTransition = popExitTransition,
  content = content
)