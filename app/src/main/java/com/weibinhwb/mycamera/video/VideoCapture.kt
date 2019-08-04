package com.weibinhwb.mycamera.video

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaFormat
import android.util.Log
import android.widget.FrameLayout
import com.weibinhwb.mycamera.App
import com.weibinhwb.mycamera.MediaData
import com.weibinhwb.mycamera.MediaDataListener
import java.io.IOException


/**
 * Created by weibin on 2019/7/24
 */


@Suppress("DEPRECATION")
class VideoCapture(private val listener: MediaDataListener) : Camera.PreviewCallback {

    private val TAG = "VideoCapture"
    private lateinit var mCamera: Camera
    private lateinit var mPreview: CameraPreview
    private val mContext = App.getInstance()
    private var isRecord = false

    private val FRAME_RAGE: Int = 30
    private var BIT_RATE: Int = -1
    private val I_FRAME_INTERVAL = 10

    private lateinit var mYuv420Sp: ByteArray
    private val mWidth = 1280
    private val mHeight = 720

    private val TIME_OUT = 10000L

    fun initCamera(frameLayout: FrameLayout) {
        Log.d(TAG, "start initCamera")
        mCamera = getCameraInstance()!!
        mCamera.setDisplayOrientation(90)
        mPreview = CameraPreview(mContext, mCamera)
        mPreview.also {
            val preview: FrameLayout = frameLayout
            preview.addView(it)
        }
        mCamera.setPreviewCallback(this)

        mCamera.apply {
            val tempParameter = parameters
            tempParameter.previewFormat = ImageFormat.NV21
            tempParameter.setPreviewSize(mWidth, mHeight)
            parameters = tempParameter
        }

        mYuv420Sp = ByteArray(mWidth * mHeight * 3 / 2)
        BIT_RATE = mWidth * mHeight * 3 * 8 * FRAME_RAGE / 256

        Log.d(TAG, "stop initCamera")
    }

    fun startRecord() {
        initVideoCodec()
        Log.d(TAG, "startRecord")
        isRecord = true
    }

    fun stopRecord() {
        isRecord = false
        mVideoCodec.stop()
        mVideoCodec.release()
        Log.d(TAG, "stopRecord")
    }

    fun releaseCamera() {
        mCamera.release()
        Log.d(TAG, "releaseCamera")
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Log.d(TAG, "open Camera !!!")
            Camera.open()
        } catch (e: Exception) {
            Log.d(TAG, "open Camera fail !!!")
            null
        }
    }

    private lateinit var mVideoCodec: MediaCodec
    private var mVideoTrackIndex = -1

    fun initVideoCodec() {
        try {
            mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            Log.d(TAG, e.message)
        }
//        val mediaCodecInfo = selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC)!!
//        val colorFormat = getColorFormat(mediaCodecInfo)
        val mediaFormat: MediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mHeight, mWidth)
//        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RAGE)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)

        mVideoCodec.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE)
        mVideoCodec.start()
    }

    private fun processVideoCodec(input: ByteArray) {
        Log.d(TAG, "start processing")
        val inputIndex = mVideoCodec.dequeueInputBuffer(TIME_OUT)
        if (inputIndex >= 0) {
            val inputBuffers = mVideoCodec.getInputBuffer(inputIndex)!!
            inputBuffers.clear()
            inputBuffers.put(input)
            Log.d(TAG, "origin: ${input.size}")
            val currentTime = System.nanoTime() / 1000
            // 入队输入的数据
            mVideoCodec.queueInputBuffer(inputIndex, 0, input.size, currentTime, 0)
            Log.d(TAG, currentTime.toString())
        }

        //bufferInfo 填充Media的信息
        var bufferInfo = MediaCodec.BufferInfo()
        var outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && mVideoTrackIndex == -1) {
            Log.d(TAG, "VideoTrackIndex = $mVideoTrackIndex")
            mVideoTrackIndex = listener.muxerStart(mVideoCodec.outputFormat)
            Log.d(TAG, "VideoTrackIndex = $mVideoTrackIndex")
        }
        while (outputIndex >= 0) {
            val outputBuffers = mVideoCodec.getOutputBuffer(outputIndex)

            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                Log.e(TAG, "vedio run: BUFFER_FLAG_CODEC_CONFIG")
                bufferInfo.size = 0
            }
            if (bufferInfo.size > 0) {
                outputBuffers?.let {
                    val bytes = ByteArray(bufferInfo.size)
                    it.get(bytes)
                    it.position(bufferInfo.offset)
                    it.limit(bufferInfo.offset + bufferInfo.size)
                    Log.e(TAG, "video presentationTimeUs : " + bufferInfo.presentationTimeUs)
//                    bufferInfo.offset = 0
//                    bufferInfo.size = input.size
//                    bufferInfo.flags = BUFFER_FLAG_KEY_FRAME
                    bufferInfo.presentationTimeUs = System.nanoTime() / 1000
                    val data = MediaData(mVideoTrackIndex, bytes, bufferInfo)
                    listener.put(data)
                }
            }
            mVideoCodec.releaseOutputBuffer(outputIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputIndex = mVideoCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        }
        Log.d(TAG, "stop processing")
    }

    private fun releaseCodec() {
        mVideoCodec.stop()
        mVideoCodec.release()

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

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (isRecord && data != null) {
            NV21toI420SemiPlanar(data, mYuv420Sp, mWidth, mHeight)
            processVideoCodec(mYuv420Sp)
        }
    }
}