package com.dji.djisdkdemo.presenter

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import dji.common.mission.waypoint.Waypoint
import dji.common.mission.waypoint.WaypointMission
import dji.common.mission.waypoint.WaypointMissionFinishedAction
import dji.common.mission.waypoint.WaypointMissionHeadingMode
import dji.common.mission.waypoint.WaypointMissionFlightPathMode
import dji.common.error.DJIError
import dji.common.mission.waypoint.WaypointMissionExecutionEvent
import dji.common.mission.waypoint.WaypointMissionUploadEvent
import dji.common.mission.waypoint.WaypointMissionDownloadEvent
import dji.common.util.CommonCallbacks
import dji.sdk.mission.waypoint.WaypointMissionOperator
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener

class WayPointMissionPresenter {
    companion object {
        const val TAG = "WayPointMissionPresenter"
    }

    enum class SPEED(val value: Float){
        LOW(3.0f),
        MIDDLE(5.0f),
        HIGH(10.0f)
    }

    private var altitude = 0.0f
    private var speed = SPEED.LOW.value

    private var waypointMissionBuilder: WaypointMission.Builder? = null
    private var instance: WaypointMissionOperator? = null

    private var finishedAction = WaypointMissionFinishedAction.NO_ACTION
    private var headingMode = WaypointMissionHeadingMode.AUTO

    fun setSpeed(spd: SPEED) {
        speed = spd.value
    }

    fun setFinishedAction(action: WaypointMissionFinishedAction) {
        finishedAction = action
    }

    fun setHeadingMode(mode: WaypointMissionHeadingMode) {
        headingMode = mode
    }

    fun setAltitude(alt: Int?) {
        alt?.let {
            altitude = it.toFloat()
        }
    }

    fun printSettings() {
        Log.d(TAG, "altitude $altitude")
        Log.d(TAG, "speed $speed")
        Log.d(TAG, "finishedAction $finishedAction")
        Log.d(TAG, "headingMode $headingMode")
    }

    fun configWayPointMission() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = WaypointMission.Builder()
                .finishedAction(finishedAction)
                .headingMode(headingMode)
                .autoFlightSpeed(speed)
                .maxFlightSpeed(speed)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
        } else {
            waypointMissionBuilder?.apply {
                finishedAction(finishedAction)
                headingMode(headingMode)
                autoFlightSpeed(speed)
                maxFlightSpeed(speed)
                flightPathMode(WaypointMissionFlightPathMode.NORMAL)
            }
        }
        waypointMissionBuilder?.let {
            if (it.waypointList.size > 0) {
                for (i in it.waypointList.indices) {
                    it.waypointList[i].altitude = altitude
                }
                Log.d(TAG, "Set Waypoint attitude successfully")
            }

            // Waypoint Load Mission
            val error = getWaypointMissionOperator()?.loadMission(it.build())
            error?.let { err ->
                Log.d(TAG, "loadWaypoint failed " + err.description)
            } ?: Log.d(TAG, "loadWaypoint succeeded")
        } ?: run {
            Log.d(TAG, "waypointMissionBuilder is null")
        }
    }

    private fun getWaypointMissionOperator(): WaypointMissionOperator? {
        if (instance == null) {
            if (DJISDKManager.getInstance().missionControl != null) {
                instance = DJISDKManager.getInstance().missionControl.waypointMissionOperator
            }
        }
        return instance
    }

    // Add Listener for WaypointMissionOperator
    fun addListener() {
        getWaypointMissionOperator()?.addListener(eventNotificationListener)
    }

    fun removeListener() {
        getWaypointMissionOperator()?.removeListener(eventNotificationListener)
    }

    private val eventNotificationListener: WaypointMissionOperatorListener =
        object : WaypointMissionOperatorListener {
            override fun onDownloadUpdate(downloadEvent: WaypointMissionDownloadEvent) = Unit
            override fun onUploadUpdate(uploadEvent: WaypointMissionUploadEvent) = Unit
            override fun onExecutionUpdate(executionEvent: WaypointMissionExecutionEvent) = Unit
            override fun onExecutionStart() = Unit
            override fun onExecutionFinish(error: DJIError?) {
                error?.let {
                    Log.e(TAG, "Execution finished: ${error.description}")
                } ?: run {
                    Log.d(TAG, "Execution finished: Success!")
                }
            }
        }

    fun addWaypoint(latLng: LatLng) {
        val waypoint = Waypoint(latLng.latitude, latLng.longitude, altitude)
        waypointMissionBuilder?.addWaypoint(waypoint) ?: run {
            waypointMissionBuilder = WaypointMission.Builder().addWaypoint(waypoint)
            waypointMissionBuilder?.build()
        }
    }

    fun uploadWayPointMission() {
        getWaypointMissionOperator()?.uploadMission(
            object : CommonCallbacks.CompletionCallback<DJIError?> {
                override fun onResult(error: DJIError?) {
                    error?.let {
                        val msg = "Mission upload failed, error: ${error.description} retrying..."
                        Log.e(TAG, msg)
                        getWaypointMissionOperator()?.retryUploadMission(null)
                    } ?: run {
                        Log.d(TAG, "Mission upload successfully!")
                    }
                }
            }
        )
    }

    fun startWaypointMission() {
        getWaypointMissionOperator()?.startMission(
            object : CommonCallbacks.CompletionCallback<DJIError?> {
            override fun onResult(error: DJIError?) {
                error?.let {
                    Log.e(TAG, "Mission Start: ${error.description}")
                } ?: run {
                    Log.d(TAG, "Mission Start: Success!")
                }
            }
        })
    }

    fun stopWaypointMission() {
        getWaypointMissionOperator()?.stopMission(
            object : CommonCallbacks.CompletionCallback<DJIError?> {
            override fun onResult(error: DJIError?) {
                error?.let {
                    Log.e(TAG, "Mission Stop: ${error.description}")
                } ?: run {
                    Log.d(TAG, "Mission Stop: Success!")
                }
            }
        })
    }
}
