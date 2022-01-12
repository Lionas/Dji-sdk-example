package com.dji.djisdkdemo.presenter

import com.dji.djisdkdemo.interfaces.MapViewControllerCallback
import dji.common.error.DJIError
import dji.sdk.sdkmanager.DJISDKManager
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.common.flightcontroller.flyzone.FlyZoneInformation
import dji.common.util.CommonCallbacks

class FlyZonePresenter(private val mapCallback: MapViewControllerCallback) {
    companion object {
        const val TAG = "FlyZonePresenter"
    }

    private val unlockFlyZoneIds = ArrayList<Int>()

    fun initFlyZone() {
        DJISDKManager.getInstance().flyZoneManager?.setFlySafeNotificationCallback { status ->
            mapCallback.showToast(status.type.name)
        }
    }

    fun printSurroundFlyZones() {
        // get fly zones from API
        DJISDKManager.getInstance().flyZoneManager?.getFlyZonesInSurroundingArea(object :
            CompletionCallbackWith<ArrayList<FlyZoneInformation?>?> {
            override fun onSuccess(flyZones: ArrayList<FlyZoneInformation?>?) {
                mapCallback.showToast("get surrounding Fly Zone Success!")
                mapCallback.updateFlyZonesOnTheMap(flyZones)
                showSurroundFlyZonesInTv(flyZones)
            }

            override fun onFailure(error: DJIError) {
                mapCallback.showToast(error.description)
            }
        })
    }

    fun showSurroundFlyZonesInTv(flyZones: List<FlyZoneInformation?>?) {
        flyZones?.let { zones ->
            val sb = StringBuffer()
            sb.append("zones counts = ${zones.size}").append("\n")
            for (flyZone in zones) {
                flyZone?.let {
                    if (it.category != null) {
                        sb.apply {
                            append("FlyZoneId: ").append(it.flyZoneID).append("\n")
                            append("Category: ").append(it.category.name).append("\n")
                            append("Latitude: ").append(it.coordinate.latitude).append("\n")
                            append("Longitude: ").append(it.coordinate.longitude).append("\n")
                            append("FlyZoneType: ").append(it.flyZoneType.name).append("\n")
                            append("Radius: ").append(it.radius).append("\n")
                            append("Shape: ").append(it.shape.name).append("\n")
                            append("StartTime: ").append(it.startTime).append("\n")
                            append("EndTime: ").append(it.endTime).append("\n")
                            append("UnlockStartTime: ").append(it.unlockStartTime).append("\n")
                            append("UnlockEndTime: ").append(it.unlockEndTime).append("\n")
                            append("Name: ").append(it.name).append("\n")
                            append("\n")
                        }
                    }

                }
            }
            mapCallback.showSurroundFlyZonesInTv(sb.toString())
        }
    }

    fun addUnlockFlyZone(id: Int) {
        unlockFlyZoneIds.add(id)
    }

    fun unlockFlyZones() {
        DJISDKManager.getInstance().flyZoneManager?.unlockFlyZones(unlockFlyZoneIds) { error ->
            unlockFlyZoneIds.clear()
            if (error == null) {
                mapCallback.showToast("unlock NFZ Success!")
            } else {
                mapCallback.showToast(error.description)
            }
        }
    }

    fun getUnlockedFlyZones() {
        // get self unlocking (TODO: custom unlockingは未対応：要調査)
        DJISDKManager.getInstance().flyZoneManager?.getUnlockedFlyZonesForAircraft(
            object : CompletionCallbackWith<List<FlyZoneInformation?>?> {
                override fun onSuccess(flyZoneInformations: List<FlyZoneInformation?>?) {
                    mapCallback.showToast("Get Unlock NFZ success")
                    showSurroundFlyZonesInTv(flyZoneInformations)
                }
                override fun onFailure(djiError: DJIError) {
                    mapCallback.showToast(djiError.description)
                }
            }
        )
    }
}