package com.weibinhwb.mycamera.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MuxerOperation
import kotlin.math.min

/**
 * Created by weibin on 2019/8/3
 */


class AudioCapture(private val listener: MediaDataListener) : MediaLifeCycle {

    private val TAG = "AudioCapture"
    //采样率，44100在所有的机器都可以保证运行
    private val mSampleRate = 44100
    //量化宽度
    private val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    //通道，双声道的时候，声音特别低。超低音
    private val mChannelConfig = AudioFormat.CHANNEL_IN_MONO
    //声音来源
    private val mSource = MediaRecorder.AudioSource.DEFAULT

    private lateinit var mAudioRecorder: AudioRecord
    private var mBufferSize: Int = 0


    override fun start() {
        Thread {
            mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat)
            mBufferSize = min(mBufferSize, 4 * 1024)
            Log.d(TAG, "mBufferSize = $mBufferSize")

            //创建AudioRecorder
            mAudioRecorder = AudioRecord(mSource, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize)
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