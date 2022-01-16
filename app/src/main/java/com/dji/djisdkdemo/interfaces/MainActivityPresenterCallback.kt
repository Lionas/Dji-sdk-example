package com.dji.djisdkdemo.interfaces

import dji.sdk.flightcontroller.FlightController

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityPresenterCallback {
    fun setStatusMessage(message: String)
    fun requestPermissions(missingPermission: MutableList<String>)
    fun setProduct(name: String)
    fun notifyStatusChange()

    //region map control
    fun updateDroneLocation(lat: Double, lng: Double)
    //endregion

    //region waypoint
    fun setEnableWayPoint(enable: Boolean)
    //endregion
}
