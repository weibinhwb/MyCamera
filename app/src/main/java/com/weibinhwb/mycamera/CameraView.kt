package com.weibinhwb.mycamera

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodec.CONFIGURE_FLAG_ENCODE
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.widget.FrameLayout
import java.io.IOException


/**
 * Created by weibin on 2019/7/24
 */


@Suppress("DEPRECATION")
object CameraView : Camera.PreviewCallback {

    private val TAG = "CameraView"
    private lateinit var mCamera: Camera
    private lateinit var mPreview: CameraPreview
    private val mContext = App.getInstance()
    private var isRecord = false

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
            tempParameter.setPreviewSize(1280, 720)
            parameters = tempParameter
        }
        Log.d(TAG, "stop initCamera")
    }

    fun startRecord() {
        initCodec()
        Log.d(TAG, "startRecord")
        isRecord = true
    }

    fun stopRecord() {
        releaseCodec()
        Log.d(TAG, "stopRecord")
        isRecord = false
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

    private lateinit var mCodec: MediaCodec

    private fun initCodec() {
        try {
            mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        } catch (e: IOException) {
            Log.d(TAG, e.message)
        }
        val mediaCodecInfo = selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC)!!
        val colorFormat = getColorFormat(mediaCodecInfo)
        val mediaFormat: MediaFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mPreview.width, mPreview.height)
//        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 300 * 1000)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)

        mCodec.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE)
        mCodec.start()
    }


    private fun processCodec(input: ByteArray) {
        Log.d(TAG, "start processing")
        val inputIndex = mCodec.dequeueInputBuffer(-1)
        if (inputIndex >= 0) {
            val inputBuffers = mCodec.getInputBuffer(inputIndex)!!
            inputBuffers.clear()
            inputBuffers.put(input)
            mCodec.queueInputBuffer(inputIndex, 0, input.size, System.currentTimeMillis(), 0)
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputIndex = mCodec.dequeueOutputBuffer(bufferInfo, 12000)
        while (outputIndex >= 0) {
            val outputBuffers = mCodec.getOutputBuffer(outputIndex)
            outputBuffers?.let {
                val tepByteArray = ByteArray(bufferInfo.size)
                it.get(tepByteArray)
                Log.d(TAG, "encoder: ${tepByteArray.size}")
            }
            mCodec.releaseOutputBuffer(outputIndex, false)
            outputIndex = mCodec.dequeueOutputBuffer(bufferInfo, 12000)
        }
        Log.d(TAG, "stop processing")
    }


    private fun getColorFormat(mediaCodecInfo: MediaCodecInfo): Int {
        var matchedFormat = 0
        val codecCapabilities: MediaCodecInfo.CodecCapabilities =
            mediaCodecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
        for (i in codecCapabilities.colorFormats.indices) {
            val format = codecCapabilities.colorFormats[i]
            if (format >= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                && format <= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
            ) {
                if (format >= matchedFormat) {
                    matchedFormat = format
                    logColorFormatName(format)
                    break
                }
            }
        }
        return matchedFormat
    }

    private fun selectCodec(mimeType: String): MediaCodecInfo? {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)

            if (!codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return null
    }


    private fun logColorFormatName(format: Int) {
        when (format) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible -> Log.d(TAG, "COLOR_FormatYUV420Flexible")
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> Log.d(
                TAG,
                "COLOR_FormatYUV420PackedPlanar"
            )
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> Log.d(TAG, "COLOR_FormatYUV420Planar")
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> Log.d(
                TAG,
                "COLOR_FormatYUV420PackedSemiPlanar"
            )
            COLOR_FormatYUV420SemiPlanar -> Log.d(TAG, "COLOR_FormatYUV420SemiPlanar")
        }
    }

    private fun releaseCodec() {
        mCodec.stop()
        mCodec.release()

    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (isRecord) {
            processCodec(data!!)
        }
    }
}