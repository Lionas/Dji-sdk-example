package com.dji.djisdkdemo

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.multidex.MultiDex
import com.secneo.sdk.Helper
import dagger.hilt.android.HiltAndroidApp
import dji.ux.beta.core.communication.DefaultGlobalPreferences
import dji.ux.beta.core.communication.GlobalPreferencesManager
import com.dji.djisdkdemo.DJIConnectionControlActivity.Companion.ACCESSORY_ATTACHED

@HiltAndroidApp
class MApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        //For the global preferences to take effect, this must be done before the widgets are initialized
        //If this is not done, no global preferences will take effect or persist across app restarts
        GlobalPreferencesManager.initialize(DefaultGlobalPreferences(this))
        val br: BroadcastReceiver = OnDJIUSBAttachedReceiver()
        val filter = IntentFilter()
        filter.addAction(ACCESSORY_ATTACHED)
        registerReceiver(br, filter)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        Helper.install(this)
    }
}