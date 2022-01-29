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
            uiController.setTextViewStatusMessage(message)
        }

        override fun requestPermissions(missingPermission: MutableList<String>) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }

        override fun setProduct(name: String) {
            uiController.setTextViewProduct(name)
        }

        override fun notifyStatusChange() {
            uiController.notifyStatusChange()
        }

        override fun updateDroneLocation(lat: Double, lng: Double) {
            uiController.updateDroneLocation(lat, lng)
        }
    }

    private val presenter = MainActivityPresenter(callback)
    private val uiController: MainActivityViewController =
        MainActivityViewController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isAppStarted = true

        // Initialize UI
        lifecycle.addObserver(uiController)
        uiController.initUI(savedInstanceState)

        // Check Require Permissions
        presenter.checkAndRequestPermissions(baseContext)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        uiController.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        uiController.onLowMemory()
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