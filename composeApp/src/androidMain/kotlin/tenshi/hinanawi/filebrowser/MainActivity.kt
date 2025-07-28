package tenshi.hinanawi.filebrowser

import android.content.Context
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
    context = applicationContext
    setApplicationContext(applicationContext)
    setContent {
      App()
    }
  }

  companion object {
    lateinit var context: Context
  }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}