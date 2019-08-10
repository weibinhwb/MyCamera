package com.weibinhwb.mycamera

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.weibinhwb.mycamera.audio.AudioDecoder
import com.weibinhwb.mycamera.video.VideoDecoder
import java.nio.ByteBuffer

/**
 * Created by weibin on 2019/8/9
 */


class ExtractOperation(private val surface: Surface, private val filePath: String) : MediaLifeCycle {

    private val TAG = "ExtractorOperation"

    private lateinit var mVideoDecoder: VideoDecoder
    private lateinit var mAudioDecoder: AudioDecoder


    override fun start() {
        mVideoDecoder = VideoDecoder(surface, filePath)
        mVideoDecoder.prepare()
        mVideoDecoder.start()
        mAudioDecoder = AudioDecoder(filePath)
        mAudioDecoder.prepare()
        mAudioDecoder.start()
    }


    override fun stop() {

    }
}