package com.weibinhwb.mycamera

/**
 * Created by weibin on 2019/8/3
 */


interface MediaListener {
    fun init()
    fun start()
    fun process()
    fun stop()
    fun release()
}