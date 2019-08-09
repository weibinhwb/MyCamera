package com.weibinhwb.mycamera

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

class VideoActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var mExtractor: ExtractOperation? = null
    private val TAG = "VideoActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        val surfaceView: SurfaceView = findViewById(R.id.video_surface)
        surfaceView.holder.addCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mExtractor?.stop()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (mExtractor == null) {
            val path = intent.getStringExtra("path")
            mExtractor = ExtractOperation(holder!!.surface, path)
            Log.d(TAG, "path = $path")
            mExtractor!!.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

}
