package com.dji.djisdkdemo.interfaces

import dji.sdk.flightcontroller.FlightController

/**
 * WayPointMissionPresenterからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface WayPointMissionPresenterCallback {
    fun getFlightController(): FlightController?
}
