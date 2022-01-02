package com.dji.djisdkdemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.dji.djisdkdemo.interfaces.MainActivityCallback
import com.dji.djisdkdemo.presenter.MainActivityPresenter

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        const val REQUEST_PERMISSION_CODE = 12345
    }

    private lateinit var mHandler: Handler
    private lateinit var mTxtStatusMessage: TextView
    private lateinit var mTxtProduct: TextView

    private val mCallback = object: MainActivityCallback {
        override fun setStatusMessage(message: String) {
            mHandler.post {
                mTxtStatusMessage.text = message
            }
        }

        override fun requestPermissions(missingPermission: MutableList<String>) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }

        override fun setProduct(name: String) {
            mHandler.post {
                mTxtProduct.text = name
            }
        }

        override fun notifyStatusChange() {
            mHandler.removeCallbacks(updateRunnable)
            mHandler.postDelayed(updateRunnable, 500)
        }
    }

    private val updateRunnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        sendBroadcast(intent)
    }

    private val mPresenter = MainActivityPresenter(mCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        mTxtStatusMessage = findViewById(R.id.txtStatusMessage)
        mTxtProduct = findViewById(R.id.txtProduct)

        mPresenter.setCallback(mCallback)
        mPresenter.checkAndRequestPermissions(baseContext)

        // Initialize DJI SDK Manager
        mHandler = Handler(Looper.getMainLooper())
    }

    /**
     * Result of runtime permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
}