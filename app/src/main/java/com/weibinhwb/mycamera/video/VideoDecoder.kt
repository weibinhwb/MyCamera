package com.weibinhwb.mycamera.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.weibinhwb.mycamera.MediaData
import com.weibinhwb.mycamera.MediaLifeCycle

/**
 * Created by weibin on 2019/8/9
 */


class VideoDecoder(private val surface: Surface, private val extractor: MediaExtractor, private val track: Int) :
    MediaLifeCycle {

    private val TAG = "VideoDecoder"
    private lateinit var mVideoCodec: MediaCodec
    private val TIME_OUT = 12000L

    override fun prepare() {
        super.prepare()
    }

    override fun start() {
        Thread {
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME)
            mVideoCodec = MediaCodec.createDecoderByType(mime)
            mVideoCodec.configure(format, surface, null, 0)
            mVideoCodec.start()
            var isStop = false
            extractor.selectTrack(track)
            while (!isStop) {
                Log.d(TAG, "weibin")
                val inputIndex = mVideoCodec.dequeueInputBuffer(TIME_OUT)
                if (inputIndex >= 0) {
                    val inputBuffers = mVideoCodec.getInputBuffer(inputIndex)!!
                    inputBuffers.clear()
                    val sampleSize = extractor.readSampleData(inputBuffers, 0)
                    if (sampleSize < 0) {
                        break
                    } else {
                        mVideoCodec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
                var bufferInfo = MediaCodec.BufferInfo()
                var outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                while (outputIndex >= 0) {
                    mVideoCodec.releaseOutputBuffer(outputIndex, true)
                    bufferInfo = MediaCodec.BufferInfo()
                    outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                }
            }
            extractor.unselectTrack(track)
        }.start()
    }

    override fun stop() {
        mVideoCodec.stop()
        mVideoCodec.release()
    }
}