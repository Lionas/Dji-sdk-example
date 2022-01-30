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

// MainActivityの処理
class MainActivityPresenter(private val activityCallback: MainActivityCallback) {

    companion object {
        const val TAG = "MainActivityPresenter"

        // 必要な権限のリスト
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

    // 権限の取得確認と権限取得の要求
    fun checkAndRequestPermissions(context: Context) {
        // パーミッションの確認
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            val permission = ContextCompat.checkSelfPermission(context, eachPermission)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 権限が得られていない項目を権限未取得リストに追加
                missingPermission.add(eachPermission)
            }
        }
        if (missingPermission.isEmpty()) {
            // 全ての権限が得られていたらSDKの登録開始
            startSDKRegistration(context)
        } else {
            // 全ての権限が得られていない場合は権限取得を要求
            activityCallback.setStatusMessage("権限の付与が必要です")
            activityCallback.requestPermissions(missingPermission)
        }
    }

    // 権限取得結果
    fun onRequestPermissionsResult(
        context: Context,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MainActivity.REQUEST_PERMISSION_CODE) {
            for (i in grantResults.size-1 downTo 0) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // 取得された権限は権限未取得リストから除外
                    missingPermission.remove(permissions[i])
                }
            }
        }
        if (missingPermission.isEmpty()) {
            // 全ての権限が得られていたらSDKの登録開始
            startSDKRegistration(context)
        } else {
            // まだ足りない場合は処理続行を諦める
            activityCallback.setStatusMessage("権限が取得されていないので開始できません")
        }
    }

    // 終了処理
    fun dispose() {
        // このアクティビティからの参照によるメモリリークを防止するためSDKを終了する
        if (djiSdkManager != null) {
            djiSdkManager?.destroy()
        }
    }

    // SDK登録開始処理
    private fun startSDKRegistration(context: Context) {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            // 登録開始処理中でなければ登録処理を開始
            registerApp(context)
        }
    }

    // SDKの登録処理
    private fun registerApp(context: Context) {
        // 非同期処理として実行
        scope.launch(Dispatchers.Default) {
            activityCallback.setStatusMessage("SDK登録中...")

            // SDKからのコールバック
            val callback = object : DJISDKManager.SDKManagerCallback {
                override fun onRegister(djiError: DJIError?) {
                    if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                        activityCallback.setStatusMessage("SDK登録完了")

                        // ドローンに接続開始
                        DJISDKManager.getInstance().startConnectionToProduct()
                    } else {
                        activityCallback.setStatusMessage("SDKの登録に失敗しました")
                    }
                    djiError?.let {
                        // ログにも詳細を出力しておく
                        Log.v(TAG, it.description)
                    }
                }
                override fun onProductDisconnect() {
                    notifyStatusChange("切断されました")
                }
                override fun onProductConnect(baseProduct: BaseProduct?) {
                    loginAccount(context)
                    notifyStatusChange("接続されました: $baseProduct")
                }

                override fun onProductChanged(baseProduct: BaseProduct?) {
                    notifyStatusChange("変更されました： $baseProduct")
                }

                override fun onComponentChange(
                    componentKey: BaseProduct.ComponentKey?,
                    oldComponent: BaseComponent?,
                    newComponent: BaseComponent?
                ) {
                    activityCallback.setStatusMessage("コンポーネントが変更されました")
                }

                override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, totalProcess: Int) {
                    val message = djisdkInitEvent.initializationState.toString()
                    activityCallback.setStatusMessage(message)
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    val progress = (100 * current / total).toInt()
                    if (progress != lastProgress) {
                        lastProgress = progress
                        val message = "安全飛行データベースのダウンロード進捗: $progress"
                        activityCallback.setStatusMessage(message)
                    }
                }
            }

            djiSdkManager = DJISDKManager.getInstance()
            djiSdkManager?.registerApp(context, callback) ?: run {
                activityCallback.setStatusMessage("DJISDKManagerがありません")
            }
        }
    }

    private fun notifyStatusChange(message: String) {
        setProduct()
        activityCallback.setStatusMessage(message)
        activityCallback.notifyStatusChange()
    }

    private fun loginAccount(context: Context) {
        UserAccountManager.getInstance().logIntoDJIUserAccount(
            context,
            object : CompletionCallbackWith<UserAccountState?> {
                override fun onSuccess(userAccountState: UserAccountState?) {
                    activityCallback.onLoginSuccess("ログイン成功")
                }

                override fun onFailure(error: DJIError) {
                    activityCallback.onLoginFailure("ログイン失敗:" + error.description)
                }
            }
        )
    }

    private fun setProduct() {
        activityCallback.setProduct(djiSdkManager?.product?.model?.displayName ?: "---")
    }
}