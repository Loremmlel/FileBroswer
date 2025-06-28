package tenshi.hinanawi.filebrowser

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToNavigation
import kotlinx.browser.document
import kotlinx.browser.window
import tenshi.hinanawi.filebrowser.util.loadRes
import tenshi.hinanawi.filebrowser.util.toByteArray

private const val resourceHanZiTTF = "./fonts/MicrosoftYaHei.ttf"

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalBrowserHistoryApi
fun main() {
    val body = document.body ?: return
    ComposeViewport(body) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontLoaded by remember { mutableStateOf(false) }
        if (fontLoaded) {
            App(onNavHostReady = { window.bindToNavigation(it) })
        } else {
            Text("Loading...")
        }

        LaunchedEffect(Unit) {
            val fontBytes = loadRes(resourceHanZiTTF).toByteArray()
            val fontFamily = FontFamily(listOf(Font("MicrosoftYaHei", fontBytes)))
            fontFamilyResolver.preload(fontFamily)
            fontLoaded = true
        }
    }
}

