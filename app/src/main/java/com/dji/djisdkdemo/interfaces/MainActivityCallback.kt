package com.dji.djisdkdemo.interfaces

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityCallback {
    fun setStatusMessage(message: String)
    fun requestPermissions(missingPermission: MutableList<String>)
    fun setProduct(name: String)
    fun notifyStatusChange(isAircraftConnected: Boolean)

    //region map control
    fun updateDroneLocation(lat: Double, lng: Double)
    //endregion
}
