package com.weibinhwb.mycamera.video

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaFormat
import android.util.Log
import com.weibinhwb.mycamera.MediaData
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MediaListener
import com.weibinhwb.mycamera.utils.getPresentationTimeUs
import com.weibinhwb.mycamera.utils.string


/**
 * Created by weibin on 2019/7/24
 */


@Suppress("DEPRECATION")
class VideoCodec(private val listener: MediaListener) : MediaDataListener, MediaLifeCycle {

    private val TAG = "VideoCodec"

    private val FRAME_RAGE: Int = 30
    private var BIT_RATE: Int = -1
    private val I_FRAME_INTERVAL = 10

    private val mWidth = 1280
    private val mHeight = 720
    private val TIME_OUT = 10000L
    private lateinit var mYuv420Sp: ByteArray
    private lateinit var mVideoCodec: MediaCodec
    private var mVideoTrackIndex = -1

    override fun start() {
        mYuv420Sp = ByteArray(mWidth * mHeight * 3 / 2)
        BIT_RATE = mWidth * mHeight * 3 * 8 * FRAME_RAGE / 256
        val mediaFormat: MediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RAGE)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, mWidth)
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, mHeight)
        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mVideoCodec.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE)
        mVideoCodec.start()
    }

    override fun stop() {
        mVideoCodec.stop()
        mVideoCodec.release()
    }

    override fun pushToCodec(array: ByteArray) {
        NV21toI420SemiPlanar(array, mYuv420Sp, mWidth, mHeight)
        processVideoCodec(mYuv420Sp)
    }

    private fun processVideoCodec(input: ByteArray) {
        Log.d(TAG, "RECORD processing")
        val inputIndex = mVideoCodec.dequeueInputBuffer(TIME_OUT)
        if (inputIndex >= 0) {
            val inputBuffers = mVideoCodec.getInputBuffer(inputIndex)!!
            inputBuffers.clear()
            inputBuffers.put(input)
            // 入队输入的数据
            mVideoCodec.queueInputBuffer(inputIndex, 0, input.size, getPresentationTimeUs(), 0)
        }

        //bufferInfo 填充Media的信息
        var bufferInfo = MediaCodec.BufferInfo()
        Log.d(TAG, bufferInfo.string())
        var outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        Log.d(TAG, bufferInfo.string())
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && mVideoTrackIndex == -1) {
            mVideoTrackIndex = listener.muxerStart(mVideoCodec.outputFormat)
        }
        while (outputIndex >= 0) {
            val outputBuffers = mVideoCodec.getOutputBuffer(outputIndex)
            if (bufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                outputBuffers?.let {
                    val bytes = ByteArray(bufferInfo.size)
                    it.get(bytes)
                    it.position(bufferInfo.offset)
                    it.limit(bufferInfo.offset + bufferInfo.size)
                    listener.put(MediaData(mVideoTrackIndex, bytes, bufferInfo))
                }
            }
            mVideoCodec.releaseOutputBuffer(outputIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        }
        Log.d(TAG, "stop processing")
    }

    private fun NV21toI420SemiPlanar(nv21bytes: ByteArray, i420bytes: ByteArray, width: Int, height: Int) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height)
        var i = width * height
        while (i < nv21bytes.size) {
            i420bytes[i] = nv21bytes[i + 1]
            i420bytes[i + 1] = nv21bytes[i]
            i += 2
        }
    }
}