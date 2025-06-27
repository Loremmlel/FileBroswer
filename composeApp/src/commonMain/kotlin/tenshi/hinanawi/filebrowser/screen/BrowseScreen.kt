package tenshi.hinanawi.filebrowser.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tenshi.hinanawi.filebrowser.data.online.OnlineFileRepository
import tenshi.hinanawi.filebrowser.viewmodel.BrowseViewModel
import tenshi.hinanawi.filebrowser.viewmodel.ViewModelFactory

@Composable
fun BrowseScreen(
    modifier: Modifier,
    viewModel: BrowseViewModel = ViewModelFactory.create(
        BrowseViewModel::class,
        OnlineFileRepository()
    )
) {

}