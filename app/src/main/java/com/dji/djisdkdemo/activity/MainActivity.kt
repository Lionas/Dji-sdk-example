package com.dji.djisdkdemo.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.interfaces.MainActivityCallback
import com.dji.djisdkdemo.presenter.MainActivityPresenter
import dji.ux.widget.FPVWidget

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        const val REQUEST_PERMISSION_CODE = 12345
    }

    private lateinit var handler: Handler
    private lateinit var txtStatusMessage: TextView
    private lateinit var txtProduct: TextView
    private lateinit var fpvWidget: FPVWidget

    private val mCallback = object: MainActivityCallback {
        override fun setStatusMessage(message: String) {
            handler.post {
                txtStatusMessage.text = message
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
            handler.post {
                txtProduct.text = name
            }
        }

        override fun notifyStatusChange() {
            handler.removeCallbacks(updateRunnable)
            handler.postDelayed(updateRunnable, 500)
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
        txtStatusMessage = findViewById(R.id.txtStatusMessage)
        txtProduct = findViewById(R.id.txtProduct)
        fpvWidget = findViewById(R.id.fpvWidget)
        fpvWidget.apply {
            setSourceCameraSideVisibility(false)
        }

        // Check Require Permissions
        mPresenter.checkAndRequestPermissions(baseContext)

        // Initialize DJI SDK Manager
        handler = Handler(Looper.getMainLooper())
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