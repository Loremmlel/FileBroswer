package tenshi.hinanawi.filebrowser.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import tenshi.hinanawi.filebrowser.contant.Route

@Composable
fun BottomNav(
    modifier: Modifier,
    navController: NavController
) {
    val itemHeightRatio = 0.8f
    val iconSize = 30.dp
    val fontSize = 14.sp
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(itemHeightRatio)
                .wrapContentWidth()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = {
                        navController.navigate(Route.MainScreen.stringRoute)
                    }),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = "主页",
                modifier = Modifier.size(iconSize)
            )
            Text("主页", fontSize = fontSize)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight(itemHeightRatio)
                .wrapContentWidth()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = {
                        navController.navigate(Route.FavoriteScreen.stringRoute)
                    }),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "收藏",
                modifier = Modifier.size(iconSize)
            )
            Text("收藏", fontSize = fontSize)
        }
    }
}