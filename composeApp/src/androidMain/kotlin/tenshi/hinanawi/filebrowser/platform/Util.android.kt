package tenshi.hinanawi.filebrowser.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap = BitmapFactory
  .decodeByteArray(this, 0, this.size)
  .asImageBitmap()