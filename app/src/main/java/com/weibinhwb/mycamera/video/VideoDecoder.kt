package com.weibinhwb.mycamera.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.weibinhwb.mycamera.MediaLifeCycle

/**
 * Created by weibin on 2019/8/9
 */


class VideoDecoder(private val surface: Surface, private val filePath: String) :
    MediaLifeCycle {

    private val TAG = "VideoDecoder"
    private lateinit var mVideoCodec: MediaCodec
    private val TIME_OUT = 12000L
    private lateinit var mExtractor: MediaExtractor
    private var mTrackIndex = -1

    override fun start() {
        Thread {
            mExtractor = MediaExtractor()
            mExtractor.setDataSource(filePath)
            for (i in 0 until mExtractor.trackCount) {
                val format = mExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video")) {
                    mVideoCodec = MediaCodec.createDecoderByType(mime)
                    mVideoCodec.configure(format, surface, null, 0)
                    mVideoCodec.start()
                    mExtractor.selectTrack(i)
                    mTrackIndex = i
                    break
                }
            }
            val startMs = System.currentTimeMillis()
            while (true) {
                Log.d(TAG, "weibin")
                val inputIndex = mVideoCodec.dequeueInputBuffer(TIME_OUT)
                if (inputIndex >= 0) {
                    val inputBuffers = mVideoCodec.getInputBuffer(inputIndex)!!
                    inputBuffers.clear()
                    val sampleSize = mExtractor.readSampleData(inputBuffers, 0)
                    if (sampleSize < 0) {
                        break
                    } else {
                        mVideoCodec.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.sampleTime, 0)
                        mExtractor.advance()
                    }
                }
                var bufferInfo = MediaCodec.BufferInfo()
                var outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                while (outputIndex >= 0) {
                    while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        Thread.sleep(50)
                    }
                    mVideoCodec.releaseOutputBuffer(outputIndex, true)
                    bufferInfo = MediaCodec.BufferInfo()
                    outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                }
            }
            mExtractor.unselectTrack(mTrackIndex)
        }.start()
    }

    override fun stop() {
        mVideoCodec.stop()
        mVideoCodec.release()
    }
}