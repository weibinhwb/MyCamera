package com.weibinhwb.mycamera.video

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.Parameters.FOCUS_MODE_AUTO
import android.hardware.Camera.Parameters.FOCUS_MODE_MACRO
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MuxerOperation
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Created by weibin on 2019/8/5
 */


class CameraCapture(
    private val weakActivity: WeakReference<Activity>,
    private val listener: MediaDataListener
) : MediaLifeCycle, Camera.PreviewCallback, SurfaceView(weakActivity.get()!!),
    SurfaceHolder.Callback {

    private val TAG = "CameraCapture"
    private lateinit var mCamera: Camera
    private val mWidth = 1280
    private val mHeight = 720
    private var degree: Int = -1

    private val mHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraCapture)
    }

    override fun prepare() {
        mCamera = getCameraInstance()!!
        mCamera.apply {
            degree = getCameraPreviewOrientation(
                weakActivity.get()!!,
                Camera.CameraInfo.CAMERA_FACING_BACK
            )
            setDisplayOrientation(degree)
            parameters.setPreviewSize(mWidth, mHeight)
            parameters.focusMode = FOCUS_MODE_AUTO
            setPreviewCallback(this@CameraCapture)
        }
        val focusMode = mCamera.parameters.focusMode
        Log.d("weibin", "focusMode = $focusMode")
    }

    override fun start() {

    }

    override fun stop() {
        mCamera.stopPreview()
    }


    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (MuxerOperation.RECORD && data != null) {
            camera!!.addCallbackBuffer(data)
            listener.pushToCodec(data, degree)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mCamera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.d(TAG, "Error setting mCamera preview: ${e.message}")
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mCamera.stopPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mHolder.surface == null) return
        mCamera.apply {
            try {
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting mCamera preview: ${e.message}")
            }
        }
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Log.d(TAG, "open Camera !!!")
            Camera.open()
        } catch (e: Exception) {
            Log.d(TAG, "open Camera fail !!!")
            null
        }
    }

    private fun getRotation(activity: Activity): Int {
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        return degrees
    }

    private fun getCameraPreviewOrientation(activity: Activity, cameraId: Int): Int {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        var result: Int
        val degrees = getRotation(activity)
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

}