# MyCamera
- 使用Camera、AudioTrack进行音视频数据的录制
- 使用MediaMuxer合成MP4文件
- 使用MediaExtractor解析MP4文件
- 使用MediaCodec对音频和视频原始数据进行编解码。
# bug
- 视频解析播放的时候出现波纹
- 进行第二次录制的时候，应用崩溃
# 改善
- 导入libyuv进行视频数据的处理
- 导入ffmpeg
- 加入美颜、滤镜效果
