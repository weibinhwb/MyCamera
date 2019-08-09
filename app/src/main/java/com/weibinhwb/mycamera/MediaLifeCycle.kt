package com.weibinhwb.mycamera

/**
 * Created by weibin on 2019/8/5
 */


interface MediaLifeCycle {
    fun prepare() {}
    fun start()
    fun stop()
    fun destroy() {}
}