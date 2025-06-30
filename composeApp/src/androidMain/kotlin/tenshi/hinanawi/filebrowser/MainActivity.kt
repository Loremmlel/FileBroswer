package tenshi.hinanawi.filebrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tenshi.hinanawi.filebrowser.platform.setApplicationContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setApplicationContext(applicationContext)
    setContent {
      App()
    }
  }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}