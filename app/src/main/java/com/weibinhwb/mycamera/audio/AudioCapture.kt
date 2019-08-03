package com.weibinhwb.mycamera.audio

import android.media.*
import android.util.Log
import com.weibinhwb.mycamera.MediaData
import com.weibinhwb.mycamera.MediaDataListener
import java.io.IOException
import kotlin.math.min

/**
 * Created by weibin on 2019/8/3
 */


class AudioCapture(val listener: MediaDataListener) {

    private val TAG = "AudioCapture"
    private val mSampleRate = 44100
    private val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val mChannelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val mSource = MediaRecorder.AudioSource.DEFAULT

    private lateinit var mAudioRecorder: AudioRecord
    private var mBufferSize: Int = 0

    private lateinit var mAudioSource: ByteArray
    private var isRecord = false

    private lateinit var mAudioCodec: MediaCodec
    private var mAudioTrackIndex = 0


    private fun initAudio() {
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat)
        mAudioRecorder = AudioRecord(mSource, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize * 2)

        if (mAudioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            return
        }
        mAudioSource = ByteArray(min(4096, mBufferSize))
        mAudioRecorder.startRecording()
    }

    fun startRecordAudio() {
        initAudio()
        initAudioEncoder()
        isRecord = true
        Thread {
            while (isRecord) {
                val size = mAudioRecorder.read(mAudioSource, 0, mAudioSource.size)
                if (size < 0) break
                val tempAudioSource = mAudioSource.copyOf()
                processAudioCodec(tempAudioSource)
            }
        }.start()
    }

    fun stopRecordAudio() {
        isRecord = false
        mAudioRecorder.stop()
        mAudioRecorder.release()
        mAudioCodec.stop()
        mAudioCodec.release()
    }

    fun releaseRecordAudio() {

    }

    private fun initAudioEncoder() {
        try {
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        } catch (e: IOException) {
            Log.d(TAG, e.message)
        }
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 30 * 1000)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mAudioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mAudioCodec.start()
    }

    private fun processAudioCodec(input: ByteArray) {
        Log.d(TAG, "start processing")
        val inputIndex = mAudioCodec.dequeueInputBuffer(-1)

        if (inputIndex >= 0) {
            val inputBuffers = mAudioCodec.getInputBuffer(inputIndex)!!
            inputBuffers.clear()
            inputBuffers.put(input)
            Log.d(TAG, "origin: ${input.size}")
            val currentTime = System.nanoTime() / 1000
            mAudioCodec.queueInputBuffer(inputIndex, 0, input.size, currentTime, 0)
            Log.d(TAG, currentTime.toString())
        }

        //bufferInfo 填充Media的信息
        var bufferInfo = MediaCodec.BufferInfo()
        var outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 12000)
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && mAudioTrackIndex == -1) {
            listener.muxerStart(mAudioCodec.outputFormat)?.let {
                mAudioTrackIndex = it
            }
        }
        while (outputIndex >= 0) {
            val outputBuffers = mAudioCodec.getOutputBuffer(outputIndex)
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                Log.e(TAG, "audio run: BUFFER_FLAG_CODEC_CONFIG")
                bufferInfo.size = 0
            }
            if (bufferInfo.size > 0) {
                outputBuffers?.let {
                    val bytes = ByteArray(bufferInfo.size)
                    it.get(bytes)
                    it.position(bufferInfo.offset)
                    it.limit(bufferInfo.offset + bufferInfo.size)
                    Log.e(TAG, "audio presentationTimeUs : " + bufferInfo.presentationTimeUs)
                    bufferInfo.presentationTimeUs = System.nanoTime() / 1000
                    val data = MediaData(mAudioTrackIndex, bytes, bufferInfo)
                    listener.put(data)
                }
            }
            mAudioCodec.releaseOutputBuffer(outputIndex, false)
            bufferInfo = MediaCodec.BufferInfo()
            outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 12000)
        }
        Log.d(TAG, "stop processing")
    }
}