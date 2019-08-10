package com.weibinhwb.mycamera.audio

import android.media.*
import android.util.Log
import com.weibinhwb.mycamera.MediaLifeCycle

/**
 * Created by weibin on 2019/8/9
 */


class AudioDecoder(private val filePath: String) : MediaLifeCycle {

    private val TAG = "AudioDecoder"
    private val TIME_OUT = 12000L

    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mAudioPlayer: AudioTrack
    private lateinit var mExtractor: MediaExtractor
    private var mTrackIndex = -1


    private val mSampleRate = 16000
    private val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val mChannelConfig = 1
    private val mSource = MediaRecorder.AudioSource.DEFAULT

    override fun start() {
        Thread {
            mExtractor = MediaExtractor()
            mExtractor.setDataSource(filePath)
            for (i in 0 until mExtractor.trackCount) {
                val format = mExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("audio")) {
                    mAudioCodec = MediaCodec.createDecoderByType(mime)
                    mAudioCodec.configure(format, null, null, 0)
                    mAudioCodec.start()
                    mExtractor.selectTrack(i)
                    mTrackIndex = i
                    break
                }
            }
            val format = mExtractor.getTrackFormat(mTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelConfig = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            Log.d(TAG, "mime = $mime, sampleRate = $sampleRate, channelConfig = $channelConfig")
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
            Log.d(TAG, "miniBufferSize = $minBufferSize")
            mAudioCodec = MediaCodec.createDecoderByType(mime)
            mAudioCodec.configure(format, null, null, 0)
            mAudioCodec.start()
            mAudioPlayer = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )
            mAudioPlayer.play()
            while (true) {
                Log.d(TAG, "weibin")
                val inputIndex = mAudioCodec.dequeueInputBuffer(TIME_OUT)
                if (inputIndex >= 0) {
                    val inputBuffers = mAudioCodec.getInputBuffer(inputIndex)!!
                    inputBuffers.clear()
                    val sampleSize = mExtractor.readSampleData(inputBuffers, 0)
                    if (sampleSize < 0) {
                        break
                    } else {
                        mAudioCodec.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.sampleTime, 0)
                        mExtractor.advance()
                    }
                }
                var bufferInfo = MediaCodec.BufferInfo()
                var outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                while (outputIndex >= 0) {
                    val outputBuffer = mAudioCodec.getOutputBuffer(outputIndex)!!
                    val outputBytes = ByteArray(bufferInfo.size)
                    outputBuffer.get(outputBytes)
                    outputBuffer.clear()
                    mAudioPlayer.write(outputBytes, 0, bufferInfo.size)
                    mAudioCodec.releaseOutputBuffer(outputIndex, false)
                    bufferInfo = MediaCodec.BufferInfo()
                    outputIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUT)
                }
            }
            mExtractor.unselectTrack(mTrackIndex)
        }.start()
    }

    override fun stop() {
        mAudioPlayer.stop()
        mAudioPlayer.release()
        mAudioCodec.stop()
        mAudioCodec.release()
    }

}