package com.weibinhwb.mycamera.video

import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaFormat
import android.util.Log
import com.weibinhwb.mycamera.*
import com.weibinhwb.mycamera.utils.getPresentationTimeUs


/**
 * Created by weibin on 2019/7/24
 */


@Suppress("DEPRECATION")
class VideoEncoder(private val listener: MediaListener) : MediaDataListener, MediaLifeCycle {

    private val TAG = "VideoEncoder"

    private val FRAME_RAGE: Int = 30
    private var BIT_RATE: Int = -1
    private val I_FRAME_INTERVAL = 10

    private val mWidth = 1280
    private val mHeight = 720
    private val TIME_OUT = 10000L
    private lateinit var mI420Yuv: ByteArray
    private lateinit var mYuvRotate: ByteArray
    private lateinit var mVideoCodec: MediaCodec
    private var mVideoTrackIndex = -1

    override fun start() {
        mI420Yuv = ByteArray(mWidth * mHeight * 3 / 2)
        mYuvRotate = ByteArray(mWidth * mHeight * 3 / 2)
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
        mVideoTrackIndex = -1
    }

    override fun destroy() {
        mVideoCodec.release()
    }

    override fun pushToCodec(array: ByteArray, degree: Int) {
        YuvHelper.nv21ToI420(array, mI420Yuv, mWidth, mHeight)
//        NV21toI420SemiPlanar(array, mI420Yuv, mWidth, mHeight)
        YuvHelper.i420Rotate(mI420Yuv, mYuvRotate, mWidth, mHeight, degree)
//        mYuvRotate = rotateYUVDegree90(mI420Yuv, mWidth, mHeight)
//        mYuvRotate = rotateYUVDegree90(array, mWidth, mHeight)
        processVideoCodec(mYuvRotate)
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
        var outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
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

    private fun rotateYUVDegree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        // Rotate the Y luma
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in 0 until imageHeight) {
                yuv[i] = data[y * imageWidth + x]
                i++
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1
        var x = imageWidth - 1
        while (x > 0) {
            for (y in 0 until imageHeight / 2) {
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
                i--
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + (x - 1)]
                i--
            }
            x -= 2
        }
        return yuv
    }
}