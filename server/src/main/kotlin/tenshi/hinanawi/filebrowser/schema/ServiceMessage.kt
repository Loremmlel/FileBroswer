package tenshi.hinanawi.filebrowser.schema

import tenshi.hinanawi.filebrowser.model.Message

enum class ServiceMessage(val message: String) {
  FavoriteParentNotFound("父收藏夹不存在"),
  FavoriteNotFound("收藏夹不存在"),
  FavoriteContainsSub("收藏夹不为空，包含子收藏夹"),
  FavoriteContainsFiles("收藏夹不为空，包含收藏文件"),
  FavoriteCanNotMoveSelf("收藏夹不能移动到自身或其子收藏夹下"),

  FavoriteFileNotFound("收藏文件不存在");

  fun toClientMessage(): Message = when(this) {
    FavoriteParentNotFound -> Message.FavoriteParentNotFound
    FavoriteNotFound -> Message.FavoriteNotFound
    FavoriteContainsSub -> Message.FavoriteContainsSub
    FavoriteContainsFiles -> Message.FavoriteContainsFiles
    FavoriteCanNotMoveSelf -> Message.FavoriteCanNotMoveSelf
    FavoriteFileNotFound -> Message.FavoriteFileNotFound
  }
}