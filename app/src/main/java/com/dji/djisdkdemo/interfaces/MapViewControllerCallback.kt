package com.dji.djisdkdemo.interfaces

import dji.common.flightcontroller.flyzone.FlyZoneInformation

/**
 * FlyZoneControllerからMapViewControllerの処理を呼び出すインタフェース
 */
interface MapViewControllerCallback {
    fun updateFlyZonesOnTheMap(flyZones: ArrayList<FlyZoneInformation?>?)
    fun showSurroundFlyZonesInTv(info: String)
    fun showToast(msg: String)
}
