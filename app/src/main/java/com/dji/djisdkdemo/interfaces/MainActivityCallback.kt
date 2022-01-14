package com.dji.djisdkdemo.interfaces

import dji.sdk.products.Aircraft

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityCallback {
    fun setStatusMessage(message: String)
    fun requestPermissions(missingPermission: MutableList<String>)
    fun setProduct(name: String)
    fun notifyStatusChange(product: Aircraft?)

    //region map control
    fun updateDroneLocation(lat: Double, lng: Double)
    //endregion
}
