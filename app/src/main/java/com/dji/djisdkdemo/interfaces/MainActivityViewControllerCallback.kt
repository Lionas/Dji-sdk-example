package com.dji.djisdkdemo.interfaces

import dji.sdk.flightcontroller.FlightController

/**
 * MainActivityViewControllerからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityViewControllerCallback {
    fun getFlightController(): FlightController?
}
