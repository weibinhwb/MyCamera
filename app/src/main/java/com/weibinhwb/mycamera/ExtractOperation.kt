package com.weibinhwb.mycamera

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.weibinhwb.mycamera.audio.AudioDecoder
import com.weibinhwb.mycamera.video.VideoDecoder
import java.nio.ByteBuffer

/**
 * Created by weibin on 2019/8/9
 */


class ExtractOperation(private val surface: Surface, private val filePath: String) : MediaLifeCycle {

    private val TAG = "ExtractorOperation"
    private lateinit var mExtrac: MediaExtractor
    private var mVideoTrack = -1
    private var mAudioTrack = -1

    private lateinit var mVideoCodec: MediaCodec
    private lateinit var mAudioCodec: MediaCodec

    private lateinit var mVideoDecoder: VideoDecoder
    private lateinit var mAudioDecoder: AudioDecoder


    override fun start() {
        mExtrac = MediaExtractor()
        mExtrac.setDataSource(filePath)
        Log.d(TAG, "filePath = $filePath")
        val tracks = mExtrac.trackCount
        Log.d(TAG, "tracks = $tracks")
        for (i in 0 until tracks) {
            val format = mExtrac.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video")) {
                mVideoTrack = i
                mVideoDecoder = VideoDecoder(surface, mExtrac, i)
                mVideoDecoder.prepare()
            } else if (mime.startsWith("audio")) {
                mAudioTrack = i
            }
            Log.d(TAG, mime)
        }
        mVideoDecoder.start()
    }


    override fun stop() {

    }
}