package com.weibinhwb.mycamera.utils

import android.util.Log

/**
 * Created by weibin on 2019/11/16
 */


object LogUtil {

    private val isLog = true

    fun d(tag: String, message: String) {
        if (isLog) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String) {
        if (isLog) {
            Log.e(tag, message)
        }
    }

}