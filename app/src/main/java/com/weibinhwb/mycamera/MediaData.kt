package com.weibinhwb.mycamera

import android.media.MediaCodec

/**
 * Created by weibin on 2019/8/3
 */


data class MediaData(val index: Int, val buffer: ByteArray, val bufferInfo: MediaCodec.BufferInfo) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaData

        if (index != other.index) return false
        if (!buffer.contentEquals(other.buffer)) return false
        if (bufferInfo != other.bufferInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + buffer.contentHashCode()
        result = 31 * result + bufferInfo.hashCode()
        return result
    }
}