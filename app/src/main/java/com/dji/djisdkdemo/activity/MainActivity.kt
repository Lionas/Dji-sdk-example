package com.dji.djisdkdemo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.app.ActivityCompat
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.interfaces.MainActivityCallback
import com.dji.djisdkdemo.presenter.MainActivityPresenter
import com.dji.djisdkdemo.ui.MainActivityViewController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        const val REQUEST_PERMISSION_CODE = 12345
        private var isAppStarted = false

        /**
         * Whether the app has started.
         *
         * @return `true` if the app has been started.
         */
        fun isStarted(): Boolean {
            return isAppStarted
        }
    }


    // call from presenter
    private val callback = object: MainActivityCallback {
        override fun setStatusMessage(message: String) {
            uiPresenter.setTextViewStatusMessage(message)
        }

        override fun requestPermissions(missingPermission: MutableList<String>) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }

        override fun setProduct(name: String) {
            uiPresenter.setTextViewProduct(name)
        }

        override fun notifyStatusChange() {
            uiPresenter.notifyStatusChange()
        }
    }

    private val presenter = MainActivityPresenter(callback)
    private lateinit var uiPresenter: MainActivityViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isAppStarted = true

        // Initialize UI
        uiPresenter = MainActivityViewController(this)
        uiPresenter.initUI()
        uiPresenter.initUxSdkUI(savedInstanceState)

        // Check Require Permissions
        presenter.checkAndRequestPermissions(baseContext)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        uiPresenter.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        uiPresenter.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        uiPresenter.onResume()
    }

    override fun onPause() {
        uiPresenter.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        presenter.dispose()
        isAppStarted = false
        super.onDestroy()
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
        presenter.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
}