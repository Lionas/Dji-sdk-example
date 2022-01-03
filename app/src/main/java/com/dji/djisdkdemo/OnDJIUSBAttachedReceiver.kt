/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.dji.djisdkdemo

import com.dji.djisdkdemo.activity.MainActivity.Companion.isStarted
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dji.sdk.sdkmanager.DJISDKManager

/**
 * This receiver will detect the USB attached event.
 * It will check if the app has been previously started.
 * If the app is already running, it will prevent a new activity from being started and bring the
 * existing activity to the top of the stack.
 */
class OnDJIUSBAttachedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isStarted().not()) {
            val startIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            startIntent?.apply {
                flags = (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                addCategory(Intent.CATEGORY_LAUNCHER)
                context.startActivity(this)
            }
        } else {
            val attachedIntent = Intent()
            attachedIntent.action = DJISDKManager.USB_ACCESSORY_ATTACHED
            context.sendBroadcast(attachedIntent)
        }
    }
}