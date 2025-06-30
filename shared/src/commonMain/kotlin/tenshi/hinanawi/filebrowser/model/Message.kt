package tenshi.hinanawi.filebrowser.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MessageSerializer::class)
enum class Message(val message: String) {
  FilesNotFound("找不到目录"),
  FilesForbidden("无权访问"),
  FilesIsNotDirectory("要查找的文件不是目录"),
  FilesDirectoryMustEmptyWhileDelete("要删除的目录必须为空"),
  FilesCannotDownloadDirectory("不能下载文件夹"),

  ImageIsNotImage("请求的文件不是图片"),

  TranscodeIsNotVideo("要转码的文件不是视频"),
  TranscodeIdUndefined("缺少转码任务id"),
  TranscodeTaskNotFound("转码任务不存在"),

  VideoTaskIdUndefined("缺少转码任务id"),
  VideoPathSegmentUndefined("缺少文件路径"),
  VideoIsNotVideo("请求的文件不是视频"),

  Success("成功"),
  Failed("失败"),
  InternalServerError("服务器内部错误");

  override fun toString() = message
}

object MessageSerializer : KSerializer<Message> {
  override fun serialize(encoder: Encoder, value: Message) {
    encoder.encodeString(value.message)
  }

  override fun deserialize(decoder: Decoder): Message {
    val value = decoder.decodeString()
    return Message.entries.find { it.message == value }
      ?: throw SerializationException("未知消息类型: $value")
  }

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("Message", PrimitiveKind.STRING)
}