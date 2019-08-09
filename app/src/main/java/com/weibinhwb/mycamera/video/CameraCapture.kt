package com.weibinhwb.mycamera.video

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import com.weibinhwb.mycamera.MediaDataListener
import com.weibinhwb.mycamera.MediaLifeCycle
import com.weibinhwb.mycamera.MuxerOperation
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Created by weibin on 2019/8/5
 */


@Suppress("DEPRECATION")
class CameraCapture(
    private val weakActivity: WeakReference<Activity>,
    private val frameLayout: FrameLayout,
    private val listener: MediaDataListener
) : MediaLifeCycle, Camera.PreviewCallback, SurfaceView(weakActivity.get()!!),
    SurfaceHolder.Callback {

    private val TAG = "CameraCapture"
    private val mCamera = getCameraInstance()!!
    private val mWidth = 1280
    private val mHeight = 720

    private val mHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraCapture)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun prepare() {
        mCamera.apply {
            val degree = getCameraPreviewOrientation(
                weakActivity.get()!!,
                Camera.CameraInfo.CAMERA_FACING_BACK
            )
            setDisplayOrientation(degree)
            val tempParameter = parameters
            tempParameter.previewFormat = ImageFormat.NV21
            tempParameter.setPreviewSize(mWidth, mHeight)
            parameters = tempParameter
            setPreviewCallback(this@CameraCapture)
            frameLayout.addView(this@CameraCapture)
//            startPreview()
        }
    }

    override fun start() {

    }

    override fun stop() {
//        mCamera.stopPreview()
    }

    override fun destroy() {
        mCamera.release()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (MuxerOperation.RECORD && data != null) {
            listener.pushToCodec(data)
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
        //前置
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }//后置
        return result
    }

}