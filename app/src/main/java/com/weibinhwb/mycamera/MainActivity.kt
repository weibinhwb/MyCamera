package com.weibinhwb.mycamera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "MainActivity"
    private lateinit var recordButton: Button
    private var isRecord = false

    private val PERMISSIONS_REQUEST_CODE = 10
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.button_capture)
        recordButton.setOnClickListener(this)

        if (!hasPermissions()) {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }

        val frameLayout: FrameLayout = findViewById(R.id.camera_preview)
        val weakActivity = WeakReference(this as Activity)
        MediaOperator.init(frameLayout, weakActivity)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaOperator.release()
    }

    private fun hasPermissions() = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.button_capture ->
                isRecord = if (!isRecord) {
                    MediaOperator.start()
                    !isRecord
                } else {
                    MediaOperator.stop()
                    !isRecord
                }
        }
    }
}

