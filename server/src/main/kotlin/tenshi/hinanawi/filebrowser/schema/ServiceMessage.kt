package tenshi.hinanawi.filebrowser.schema

enum class ServiceMessage(val message: String) {
  FavoriteParentNotFound("父收藏夹不存在"),
  FavoriteNotFound("收藏夹不存在"),
  FavoriteContainsSub("收藏夹不为空，包含子收藏夹"),
  FavoriteContainsFiles("收藏夹不为空，包含收藏文件"),
  FavoriteCanNotMoveSelf("收藏夹不能移动到自身或其子收藏夹下"),

  FavoriteFileNotFound("收藏文件不存在")
}