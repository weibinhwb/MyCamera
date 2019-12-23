package com.weibinhwb.mycamera.audio

import android.media.*
import android.media.AudioAttributes.CONTENT_TYPE_MOVIE
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioTrack.MODE_STREAM
import android.util.Log
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.utils.LogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception

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

    private fun init() {
        mExtractor = MediaExtractor()
        LogUtil.d(TAG, "    path = $filePath")
        val file = File(filePath)
        val fis = FileInputStream(file)
        val fd = fis.fd
        mExtractor.setDataSource(fd)
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
        fis.close()
        val format = mExtractor.getTrackFormat(mTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelConfig = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val encodingPcm = ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encodingPcm)
        mAudioCodec = MediaCodec.createDecoderByType(mime)
        mAudioCodec.configure(format, null, null, 0)
        mAudioCodec.start()
        val attributes = AudioAttributes.Builder().setContentType(CONTENT_TYPE_MOVIE).setUsage(USAGE_MEDIA)
            .setLegacyStreamType(STREAM_MUSIC).build()
        val audioFormat = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(ENCODING_PCM_16BIT)
            .setChannelMask(1).setChannelIndexMask(1)
            .build()
        mAudioPlayer = AudioTrack(attributes, audioFormat, minBufferSize, MODE_STREAM, AUDIO_SESSION_ID_GENERATE)
        mAudioPlayer.play()
    }

    override fun start() {
        Thread {
            init()
            while (true) {
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