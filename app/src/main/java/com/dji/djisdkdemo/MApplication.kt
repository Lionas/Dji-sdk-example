package com.dji.djisdkdemo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.secneo.sdk.Helper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MApplication: Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        Helper.install(this)
    }
}