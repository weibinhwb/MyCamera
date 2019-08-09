package com.weibinhwb.mycamera.utils

import android.media.MediaCodec
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by weibin on 2019/8/5
 */


fun MediaCodec.BufferInfo.string(): String {
    return """size = ${this.size} offset = ${this.offset} presentationTimeUs = ${this.presentationTimeUs} flags = ${this.flags}""".trimIndent()
}

fun getPresentationTimeUs(): Long {
    return System.nanoTime() / 1000
}


val MEDIA_TYPE_IMAGE = 1
val MEDIA_TYPE_VIDEO = 2
fun getOutputMediaFileUri(type: Int): Uri {
    return Uri.fromFile(getOutputMediaFile(type))
}

fun getOutputMediaFile(type: Int): File? {

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