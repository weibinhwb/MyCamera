package com.weibinhwb.mycamera

import android.app.Activity
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.widget.FrameLayout
import com.weibinhwb.mycamera.audio.AudioCapture
import com.weibinhwb.mycamera.audio.AudioEncoder
import com.weibinhwb.mycamera.utils.MEDIA_TYPE_VIDEO
import com.weibinhwb.mycamera.utils.getOutputMediaFile
import com.weibinhwb.mycamera.video.CameraCapture
import com.weibinhwb.mycamera.video.VideoEncoder
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

/**
 * Created by weibin on 2019/8/3
 */


class MuxerOperation(
    private val weakActivity: WeakReference<Activity>,
    private val frameLayout: FrameLayout
) : MediaListener, MediaLifeCycle {

    private val TAG = "MuxerOperation"

    lateinit var mStorePath: String
    private lateinit var mMuxer: MediaMuxer

    private lateinit var mVideoEncoder: VideoEncoder
    private lateinit var mCameraCapture: CameraCapture
    private lateinit var mAudioCapture: AudioCapture
    private lateinit var mAudioEncoder: AudioEncoder

    private val blockingQueue: ArrayBlockingQueue<MediaData> = ArrayBlockingQueue(100)

    companion object {
        @Volatile
        var RECORD = false
            @Synchronized get() {
                return field
            }
            @Synchronized set(value) {
                field = value
            }
    }

    override fun prepare() {
        mStorePath = getOutputMediaFile(MEDIA_TYPE_VIDEO)!!.absolutePath
        mMuxer = MediaMuxer(
            mStorePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        mVideoEncoder = VideoEncoder(this@MuxerOperation)
        mAudioEncoder = AudioEncoder(this@MuxerOperation)
        mAudioCapture = AudioCapture(mAudioEncoder)
        mCameraCapture = CameraCapture(weakActivity, mVideoEncoder)

        frameLayout.addView(mCameraCapture)

        mCameraCapture.prepare()
    }

    override fun start() {
        mVideoEncoder.prepare()
        mVideoEncoder.start()

        mAudioEncoder.prepare()
        mAudioEncoder.start()

        mAudioCapture.prepare()
        mAudioCapture.start()

        mCameraCapture.start()

        Thread {
            while (true) {
                if (!blockingQueue.isEmpty()) {
                    val data = blockingQueue.poll()
                    mMuxer.writeSampleData(data.index, ByteBuffer.wrap(data.buffer), data.bufferInfo)
                } else {
                    Thread.sleep(300)
                }
                if (!RECORD && blockingQueue.isEmpty()) {
                    break
                }
            }
            mMuxer.stop()
        }.start()
        Log.d("dd", "start")
    }

    override fun stop() {
        frameLayout.removeAllViews()
        mAudioCapture.stop()
        mAudioEncoder.stop()
        mVideoEncoder.stop()
        mCameraCapture.stop()
        blockingQueue.clear()
    }

    override fun destroy() {
        mMuxer.release()
    }


    override fun put(data: MediaData) {
        blockingQueue.put(data)
    }

    override fun muxerStart(mediaFormat: MediaFormat): Int {
        Log.d(TAG, "muxer $mMuxer")
        val ans = mMuxer.addTrack(mediaFormat)
        Log.d(TAG, "ans = $ans")
        if (ans == 1) {
            mMuxer.start()
        }
        return ans
    }
}