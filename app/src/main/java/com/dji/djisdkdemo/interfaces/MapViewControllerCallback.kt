package com.dji.djisdkdemo.interfaces

import com.google.android.gms.maps.model.LatLng

/**
 * MapViewControllerからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface MapViewControllerCallback {
    fun onMapClick()
    fun addWaypoint(latLng: LatLng)
}
