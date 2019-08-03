package com.weibinhwb.mycamera

import android.app.Application

/**
 * Created by weibin on 2019/8/2
 */


class App : Application() {


    companion object {
        private lateinit var instance: App
        fun getInstance(): App {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}