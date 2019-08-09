package com.weibinhwb.mycamera.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MuxerOperation
import java.lang.RuntimeException

/**
 * Created by weibin on 2019/8/3
 */


class AudioCapture(private val listener: MediaDataListener) : MediaLifeCycle {

    private val TAG = "AudioCapture"
    private val mSampleRate = 16000
    private val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val mChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val mSource = MediaRecorder.AudioSource.DEFAULT

    private lateinit var mAudioRecorder: AudioRecord
    private var mBufferSize: Int = 0


    override fun start() {
        Thread {
            mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat)
            mAudioRecorder = AudioRecord(mSource, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize * 2)
            if (mAudioRecorder.state != AudioRecord.STATE_INITIALIZED) {
                throw RuntimeException("运行错误")
            }
            mAudioRecorder.startRecording()
            val bufferBytes = ByteArray(mBufferSize);
            while (MuxerOperation.RECORD) {
                val size = mAudioRecorder.read(bufferBytes, 0, mBufferSize)
                if (size > 0)
                    listener.pushToCodec(bufferBytes)
            }
        }.start()
    }

    override fun stop() {
        try {
            mAudioRecorder.stop()
            mAudioRecorder.release()
        } catch (ie: IllegalStateException) {
            ie.printStackTrace()
        }
    }
}