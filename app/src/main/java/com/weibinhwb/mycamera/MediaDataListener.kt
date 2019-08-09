package com.weibinhwb.mycamera

/**
 * Created by weibin on 2019/8/5
 */


interface MediaDataListener {
    fun pushToCodec(array: ByteArray)
}