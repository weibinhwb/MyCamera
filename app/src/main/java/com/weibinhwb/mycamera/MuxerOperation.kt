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
import com.weibinhwb.mycamera.utils.string
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

    val mStorePath = getOutputMediaFile(MEDIA_TYPE_VIDEO)!!.absolutePath
    private val mMuxer: MediaMuxer =
        MediaMuxer(
            mStorePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

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
        mVideoEncoder = VideoEncoder(this@MuxerOperation)
        mAudioEncoder = AudioEncoder(this@MuxerOperation)
        mAudioCapture = AudioCapture(mAudioEncoder)
        mCameraCapture = CameraCapture(weakActivity, frameLayout, mVideoEncoder)

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
                    Log.d("weibin", "data.buffer size = ${data.buffer.size}")
                    Log.d("weibin", "data.index = ${data.index}")
                    Log.d("weibin", "data.bufferInfo = ${data.bufferInfo.string()}")
                    mMuxer.writeSampleData(data.index, ByteBuffer.wrap(data.buffer), data.bufferInfo)
                } else {
                    Thread.sleep(300)
                }
                if (!RECORD && blockingQueue.isEmpty()) {
                    break
                }
            }
            mMuxer.stop()
            mMuxer.release()
        }.start()
    }

    override fun stop() {
        mCameraCapture.stop()
        mAudioCapture.stop()
        mAudioCapture.stop()
        mAudioEncoder.stop()
    }

    override fun destroy() {
        mCameraCapture.destroy()
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