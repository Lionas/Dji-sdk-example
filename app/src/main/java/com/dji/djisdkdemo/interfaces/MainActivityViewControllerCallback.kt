package com.dji.djisdkdemo.interfaces

/**
 * MapViewControllerからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface MainActivityViewControllerCallback {
    fun onMapClick()
    fun showSurroundFlyZonesInTv(info: String)
    fun showToast(msg: String)
}
