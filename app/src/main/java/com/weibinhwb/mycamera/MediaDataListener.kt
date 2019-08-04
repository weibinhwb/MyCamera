package com.weibinhwb.mycamera

import android.media.MediaFormat

/**
 * Created by weibin on 2019/8/3
 */


interface MediaDataListener {
    fun put(data: MediaData)
    fun muxerStart(mediaFormat: MediaFormat): Int
}