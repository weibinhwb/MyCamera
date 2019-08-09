package com.weibinhwb.mycamera

import android.media.MediaFormat
import android.util.Log

/**
 * Created by weibin on 2019/8/3
 */


interface MediaListener {
    fun put(data: MediaData) {
    }
    fun muxerStart(mediaFormat: MediaFormat): Int
}