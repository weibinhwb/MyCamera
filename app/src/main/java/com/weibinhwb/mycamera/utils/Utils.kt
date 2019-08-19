package com.weibinhwb.mycamera.utils

import android.annotation.SuppressLint
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
val MEDIA_TYPE_AUDIO = 3
fun getOutputMediaFileUri(type: Int): Uri {
    return Uri.fromFile(getOutputMediaFile(type))
}

@SuppressLint("SimpleDateFormat")
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
        MEDIA_TYPE_AUDIO -> {
            File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.aac")
        }
        else -> null
    }
}

fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
    val profile = 2 // AAC LC
    val freqIdx = 8 //8 标识16000，取特定
    val channelCfg = 2 // 音频声道数为两个

    // fill in ADTS data
    packet[0] = 0xFF.toByte()//1111 1111
    packet[1] = 0xF9.toByte()//1111 1001  1111 还是syncword
    // 1001 第一个1 代表MPEG-2,接着00为常量，最后一个1，标识没有CRC

    packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (channelCfg shr 2)).toByte()
    packet[3] = ((channelCfg and 3 shl 6) + (packetLen shr 11)).toByte()
    packet[4] = (packetLen and 0x7FF shr 3).toByte()
    packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
    packet[6] = 0xFC.toByte()
}