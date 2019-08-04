package com.weibinhwb.mycamera

import android.app.Activity
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.FrameLayout
import com.weibinhwb.mycamera.audio.AudioCapture
import com.weibinhwb.mycamera.video.VideoCapture
import java.io.File
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

/**
 * Created by weibin on 2019/8/3
 */


object MediaOperator : MediaDataListener {

    private val TAG = "MediaOperator"
    val MEDIA_TYPE_IMAGE = 1
    val MEDIA_TYPE_VIDEO = 2
    private val mMuxer: MediaMuxer =
        MediaMuxer(getOutputMediaFile(MEDIA_TYPE_VIDEO)!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private lateinit var mVideoCapture: VideoCapture
    private lateinit var mAudioCapture: AudioCapture

    private var isRecord = false

    private val blockingQueue: ArrayBlockingQueue<MediaData> = ArrayBlockingQueue(100)


    fun init(frameLayout: FrameLayout, weakActivity: WeakReference<Activity>) {
        mVideoCapture = VideoCapture(this, weakActivity)
        mAudioCapture = AudioCapture(this)
        mVideoCapture.initCamera(frameLayout)
    }

    fun start() {
        isRecord = true
        mVideoCapture.startRecord()
        mAudioCapture.startRecordAudio()
        Thread {
            while (true) {
                if (!blockingQueue.isEmpty()) {
                    val data = blockingQueue.poll()
                    Log.d(TAG, "index = ${data.index}, info = ${data.bufferInfo.size}")
                    mMuxer.writeSampleData(data.index, ByteBuffer.wrap(data.buffer), data.bufferInfo)
                } else {
                    Thread.sleep(300)
                }
                if (!isRecord && blockingQueue.isEmpty()) {
                    break
                }
            }
            mMuxer.stop()
            mMuxer.release()
        }.start()
    }

    fun stop() {
        mVideoCapture.stopRecord()
        mAudioCapture.stopRecordAudio()
        isRecord = false
    }

    fun release() {
        mVideoCapture.releaseCamera()
        mAudioCapture.releaseRecordAudio()
    }

    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    private fun getOutputMediaFile(type: Int): File? {

        val mediaStorageDir = File(
            Environment.getExternalStorageDirectory(),
            "MyCamera"
        )

        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("MyCamera", "failed to create directory")
                    return null
                }
            }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
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
            isRecord = true
        }
        return ans
    }
}