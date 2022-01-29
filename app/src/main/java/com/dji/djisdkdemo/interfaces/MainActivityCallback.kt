package com.dji.djisdkdemo.interfaces

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityCallback {
    fun showToast(message: String)
    fun setStatusMessage(message: String)
    fun requestPermissions(missingPermission: MutableList<String>)
    fun setProduct(name: String)
    fun notifyStatusChange()

    //region map control
    fun updateDroneLocation(lat: Double, lng: Double)
    //endregion

    // ログイン成功時
    fun onLoginSuccess()
}
