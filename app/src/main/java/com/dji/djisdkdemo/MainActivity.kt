package com.dji.djisdkdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        const val REQUEST_PERMISSION_CODE = 12345

        var mProduct: BaseProduct? = null
        var mDjiSdkManager: DJISDKManager? = null

        val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private lateinit var mHandler: Handler
    private lateinit var mTxtStatusMessage: TextView
    private lateinit var mTxtProduct: TextView

    private var missingPermission = mutableListOf<String>()
    private var isRegistrationInProgress = AtomicBoolean(false)

    val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        mTxtStatusMessage = findViewById(R.id.txtStatusMessage)
        mTxtProduct = findViewById(R.id.txtProduct)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions()
        }

        // Initialize DJI SDK Manager
        mHandler = Handler(Looper.getMainLooper())
    }

    private fun checkAndRequestPermissions() {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            val permission = ContextCompat.checkSelfPermission(this, eachPermission)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            setStatusMessage("Need to grant the permissions!")
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
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
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.size-1 downTo 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            setStatusMessage("Missing permissions!!!")
        }
    }

    private fun startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            registerApp()
        }
    }

    private fun registerApp() {
        scope.launch(Dispatchers.Default) {
            setStatusMessage("registering, pls wait...")

            val callback = object : SDKManagerCallback {
                override fun onRegister(djiError: DJIError?) {
                    if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                        setStatusMessage("Register Success")
                        DJISDKManager.getInstance().startConnectionToProduct()
                    } else {
                        setStatusMessage("Register sdk fails, please check the bundle id and network connection!")
                    }
                    djiError?.let {
                        Log.v(TAG, it.description)
                    }
                }
                override fun onProductDisconnect() {
                    Log.d(TAG, "onProductDisconnect")
                    setStatusMessage("Product Disconnected")
                    setProduct()
                    notifyStatusChange()
                }
                override fun onProductConnect(baseProduct: BaseProduct?) {
                    Log.d(TAG, "onProductConnect newProduct:$baseProduct")
                    val product = mDjiSdkManager?.product
                    setStatusMessage("Product connected. product=$product")
                    setProduct()
                    notifyStatusChange()
                }

                override fun onProductChanged(baseProduct: BaseProduct?) {
                    Log.d(TAG, "onProductChanged")
                    setStatusMessage("Product changed newProduct:$baseProduct")
                    setProduct()
                }

                override fun onComponentChange(
                    componentKey: BaseProduct.ComponentKey?,
                    oldComponent: BaseComponent?,
                    newComponent: BaseComponent?
                ) {
                    newComponent?.let {
                        it.setComponentListener {
                            Log.d(TAG, "onComponentConnectivityChanged: $it")
                            setStatusMessage("onComponentConnectivityChanged $it")
                            setProduct()
                            notifyStatusChange()
                        }
                    }
                    Log.d(TAG, "onComponentChange: key:$componentKey, old:$oldComponent, new:$newComponent")
                }
                override fun onInitProcess(p0: DJISDKInitEvent?, p1: Int) = Unit
                override fun onDatabaseDownloadProgress(p0: Long, p1: Long) = Unit
            }

            mDjiSdkManager = DJISDKManager.getInstance()
            mDjiSdkManager?.registerApp(applicationContext, callback) ?: run {
                setStatusMessage("mDjiSdkManager is null!!")
            }
        }
    }

    private fun notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable)
        mHandler.postDelayed(updateRunnable, 500)
    }

    private val updateRunnable = Runnable {
        val intent = Intent(FLAG_CONNECTION_CHANGE)
        sendBroadcast(intent)
    }

    private fun showToast(message: String) {
        mHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setStatusMessage(message: String) {
        mHandler.post {
            mTxtStatusMessage.text = message
        }
    }

    private fun setProduct() {
        mHandler.post {
            mTxtProduct.text = mDjiSdkManager?.product?.model?.displayName ?: "(none)"
        }
    }
}