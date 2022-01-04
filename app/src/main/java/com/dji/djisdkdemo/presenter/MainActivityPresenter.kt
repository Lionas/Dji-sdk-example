package com.dji.djisdkdemo.presenter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dji.djisdkdemo.activity.MainActivity
import com.dji.djisdkdemo.interfaces.MainActivityCallback
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import dji.common.useraccount.UserAccountState

import dji.common.util.CommonCallbacks.CompletionCallbackWith

import dji.sdk.useraccount.UserAccountManager

class MainActivityPresenter(private val activityCallback: MainActivityCallback) {
    companion object {
        const val TAG = "MainActivityPresenter"
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

    private var djiSdkManager: DJISDKManager? = null
    private var missingPermission = mutableListOf<String>()
    private var isRegistrationInProgress = AtomicBoolean(false)

    private var lastProgress = -1
    private val scope = CoroutineScope(Dispatchers.Default)

    fun checkAndRequestPermissions(context: Context) {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            val permission = ContextCompat.checkSelfPermission(context, eachPermission)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration(context)
        } else {
            activityCallback.setStatusMessage("Need to grant the permissions!")
            activityCallback.requestPermissions(missingPermission)
        }
    }

    fun onRequestPermissionsResult(
        context: Context,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Check for granted permission and remove from missing list
        if (requestCode == MainActivity.REQUEST_PERMISSION_CODE) {
            for (i in grantResults.size-1 downTo 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration(context)
        } else {
            activityCallback.setStatusMessage("Missing permissions!!!")
        }
    }

    fun dispose() {
        // Prevent memory leak by releasing DJISDKManager's references to this activity
        if (djiSdkManager != null) {
            djiSdkManager?.destroy()
        }
    }

    private fun startSDKRegistration(context: Context) {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            registerApp(context)
        }
    }

    private fun registerApp(context: Context) {
        scope.launch(Dispatchers.Default) {
            activityCallback.setStatusMessage("registering, pls wait...")

            val callback = object : DJISDKManager.SDKManagerCallback {
                override fun onRegister(djiError: DJIError?) {
                    if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                        activityCallback.setStatusMessage("Register Success")
                        DJISDKManager.getInstance().startConnectionToProduct()
                        loginAccount(context)
                    } else {
                        activityCallback.setStatusMessage("Register sdk fails, please check the bundle id and network connection!")
                    }
                    djiError?.let {
                        Log.v(TAG, it.description)
                    }
                }
                override fun onProductDisconnect() {
                    Log.d(TAG, "onProductDisconnect")
                    activityCallback.setStatusMessage("Product Disconnected")
                    setProduct()
                    activityCallback.notifyStatusChange()
                }
                override fun onProductConnect(baseProduct: BaseProduct?) {
                    Log.d(TAG, "onProductConnect newProduct:$baseProduct")
                    activityCallback.setStatusMessage("Product connected")
                    setProduct()
                    activityCallback.notifyStatusChange()
                }

                override fun onProductChanged(baseProduct: BaseProduct?) {
                    Log.d(TAG, "onProductChanged")
                    activityCallback.setStatusMessage("Product changed")
                    setProduct()
                }

                override fun onComponentChange(
                    componentKey: BaseProduct.ComponentKey?,
                    oldComponent: BaseComponent?,
                    newComponent: BaseComponent?
                ) {
                    newComponent?.let {
                        it.setComponentListener {
                            Log.d(TAG, "onComponentChange: $it")
                        }
                    }
                    activityCallback.setStatusMessage("onComponentChanged")
                    setProduct()
                    activityCallback.notifyStatusChange()
                    Log.d(TAG, "onComponentChange: key:$componentKey, old:$oldComponent, new:$newComponent")
                }

                override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, totalProcess: Int) {
                    Log.d(TAG, djisdkInitEvent.initializationState.toString())
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    val progress = (100 * current / total).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        val message = "Fly safe database download progress: $progress"
                        Log.d(TAG, message)
                        activityCallback.setStatusMessage(message)
                    }
                }
            }

            djiSdkManager = DJISDKManager.getInstance()
            djiSdkManager?.registerApp(context, callback) ?: run {
                activityCallback.setStatusMessage("mDjiSdkManager is null!!")
            }
        }
    }

    private fun loginAccount(context: Context) {
        UserAccountManager.getInstance().logIntoDJIUserAccount(
            context,
            object : CompletionCallbackWith<UserAccountState?> {
                override fun onSuccess(userAccountState: UserAccountState?) {
                    Log.d(TAG, "Login Success")
                }

                override fun onFailure(error: DJIError) {
                    Log.e(TAG, "Login Error:" + error.description)
                }
            }
        )
    }

    private fun setProduct() {
        activityCallback.setProduct(djiSdkManager?.product?.model?.displayName ?: "(none)")
    }
}