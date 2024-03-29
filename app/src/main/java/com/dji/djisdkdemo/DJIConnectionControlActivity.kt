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

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.hardware.usb.UsbManager
import android.view.View

/**
 * Controls the connection to the USB accessory. This activity listens for the USB attached action
 * and sends a broadcast with an internal code which is listened to by the
 * [OnDJIUSBAttachedReceiver].
 */
class DJIConnectionControlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
        val usbIntent = intent
        if (usbIntent != null) {
            val action = usbIntent.action
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == action) {
                val attachedIntent = Intent()
                attachedIntent.action = ACCESSORY_ATTACHED
                sendBroadcast(attachedIntent)
            }
        }
        finish()
    }

    companion object {
        const val ACCESSORY_ATTACHED = "dji.ux.beta.sample.ACCESSORY_ATTACHED"
    }
}