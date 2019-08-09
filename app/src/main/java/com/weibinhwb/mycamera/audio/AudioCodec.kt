package com.weibinhwb.mycamera.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.weibinhwb.mycamera.MediaData
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MediaListener
import com.weibinhwb.mycamera.utils.getPresentationTimeUs
import com.weibinhwb.mycamera.utils.string
import java.io.IOException

/**
 * Created by weibin on 2019/8/6
 */


class AudioCodec(private val listener: MediaListener) : MediaLifeCycle, MediaDataListener {

    private val TAG = "AudioCodec"
    private lateinit var mAudioCodec: MediaCodec
    private val mSampleRate = 16000
    private var mAudioTrackIndex = -1

    private val TIME_OUT = 10000L


    override fun start() {
        try {
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        } catch (e: IOException) {
            Log.d(TAG, e.message)
        }
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mAudioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mAudioCodec.start()
    }

    override fun stop() {
        try {
            mAudioCodec.stop()
            mAudioCodec.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun pushToCodec(array: ByteArray) {
        processAudioCodec(array)
    }

    private fun processAudioCodec(input: ByteArray) {
        Log.d(TAG, "RECORD processing")
        val inputIndex = mAudioCodec.dequeueInputBuffer(TIME_OUT)
        if (inputIndex >= 0) {
            val inputBuffers = mAudioCodec.getInputBuffer(inputIndex)
            inputBuffers?.apply {
                clear()
                put(input)
                mAudioCodec.queueInputBuffer(inputIndex, 0, input.size, getPresentationTimeUs(), 0)
            }
        }

        //bufferInfo 填充Media的信息
        var bufferInfo = MediaCodec.BufferInfo()
        Log.d(TAG, bufferInfo.string())
        var outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        Log.d(TAG, bufferInfo.string())
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && mAudioTrackIndex == -1) {
            mAudioTrackIndex = listener.muxerStart(mAudioCodec.outputFormat)
        }
        while (outputIndex >= 0) {
            val outputBuffers = mAudioCodec.getOutputBuffer(outputIndex)
            if (bufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                outputBuffers?.let {
                    val bytes = ByteArray(bufferInfo.size)
                    it.get(bytes)
                    it.position(bufferInfo.offset)
                    it.limit(bufferInfo.offset + bufferInfo.size)
                    listener.put(MediaData(mAudioTrackIndex, bytes, bufferInfo))
                }
            }
            mAudioCodec.releaseOutputBuffer(outputIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
        }
        Log.d(TAG, "stop processing")
    }
}