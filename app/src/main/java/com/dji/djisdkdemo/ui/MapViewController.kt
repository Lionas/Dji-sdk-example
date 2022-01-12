package com.dji.djisdkdemo.ui

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dji.djisdkdemo.interfaces.MainActivityViewControllerCallback
import com.dji.djisdkdemo.interfaces.MapViewControllerCallback
import com.dji.djisdkdemo.presenter.FlyZonePresenter
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import dji.common.flightcontroller.flyzone.FlyZoneInformation
import dji.log.DJILog
import dji.common.flightcontroller.flyzone.SubFlyZoneShape
import dji.common.flightcontroller.flyzone.FlyZoneCategory
import dji.common.model.LocationCoordinate2D
import android.graphics.Color
import java.lang.ref.WeakReference
import dji.midware.data.forbid.FlyForbidProtocol
import androidx.annotation.ColorInt




class MapViewController(
    appCompatActivity: AppCompatActivity?,
    private val callback: MainActivityViewControllerCallback
) :
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener
{
    companion object {
        const val TAG = "MapViewController"

        // TODO インスタンス化も検討する
        fun checkGpsCoordinates(latitude: Double, longitude: Double): Boolean {
            return latitude > -90 && latitude < 90 &&
                    longitude > -180 && longitude < 180 &&
                    latitude != 0.0 && longitude != 0.0
        }
    }

    private val weakActivityReference = WeakReference(appCompatActivity)

    private lateinit var flyZonePresenter: FlyZonePresenter

    private var gMap: GoogleMap? = null
    private var droneLocationLat: Double = 0.0 // init value
    private var droneLocationLng: Double = 0.0 // init value
    private var droneMarker: Marker? = null
    private val cameraZoomLevel = 16.0f
    private val initMapPosition = LatLng(35.473969,140.222304) // Chiba, Japan

    // 初めて正しいGPSを受信したかどうか
    private var isFirstReceiveCorrectGps = false

    override fun onMapReady(googleMap: GoogleMap) {
        if (gMap == null) {
            gMap = googleMap
            setUpMap()
        }
        gMap?.let {
            droneLocationLat = initMapPosition.latitude
            droneLocationLng = initMapPosition.longitude
            val markerOptions = makeMarkerOptions()
            droneMarker = gMap?.addMarker(markerOptions)
            cameraUpdate()

            // FlyZoneの表示
            flyZonePresenter.printSurroundFlyZones()
        }
    }

    private fun setUpMap() {
        gMap?.let {
            it.setOnMapClickListener(this)
            it.uiSettings.isZoomControlsEnabled = false
            it.uiSettings.isMapToolbarEnabled = false
        }
    }

    override fun onMapClick(latlng: LatLng) {
        callback.onMapClick()
    }

    fun setDroneLocation(lat: Double, lng: Double) {
        droneLocationLat = lat
        droneLocationLng = lng
    }

    // update drone location
    fun updateDroneLocation() {
        val markerOptions = makeMarkerOptions()
        weakActivityReference.get()?.let {
            it.runOnUiThread {
                droneMarker?.remove()
                if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap?.addMarker(markerOptions)
                    if (isFirstReceiveCorrectGps.not()) {
                        // 初回のみ地図の位置を変更する
                        isFirstReceiveCorrectGps = true
                        cameraUpdate()
                    }
                }
            }
        }
    }

    private fun makeMarkerOptions(): MarkerOptions {
        val pos = LatLng(droneLocationLat, droneLocationLng)
        val markerOptions = MarkerOptions()
        markerOptions.position(pos)
        val droneIcon = BitmapDescriptorFactory.fromResource(com.dji.djisdkdemo.R.mipmap.ic_drone)
        markerOptions.icon(droneIcon)
        return markerOptions
    }

    fun cameraUpdate() {
        val pos = LatLng(droneLocationLat, droneLocationLng)
        val cu: CameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, cameraZoomLevel)
        gMap?.moveCamera(cu)
    }

    //region fly zone
    private var unlockableIds: ArrayList<Int> = ArrayList()
    private val warningFillColor = Color.argb(16, 0x1E, 0xFF, 0x00)
    private val limitFillColor = Color.HSVToColor(120, floatArrayOf(0f, 1f, 1f))
    private val limitCanUnLimitFillColor = Color.argb(40, 0xFF, 0xFF, 0x00)
    private val painter: FlyFrbBasePainter = FlyFrbBasePainter()

    private fun setUpFlyZone() {
        flyZonePresenter = FlyZonePresenter(object: MapViewControllerCallback {
            override fun updateFlyZonesOnTheMap(flyZones: ArrayList<FlyZoneInformation?>?) {
                flyZones?.let {
                    updateFlyZones(it)
                }
            }

            override fun showSurroundFlyZonesInTv(info: String) {
                weakActivityReference.get()?.let {
                    it.runOnUiThread {
                        // TODO UIに表示する
                        callback.showSurroundFlyZonesInTv(info)
                        Log.d(TAG, info)
                    }
                }
            }

            override fun showToast(msg: String) {
                callback.showToast(msg)
            }
        })
    }

    fun printSurroundFlyZones() {
        // FlyZoneの表示
        flyZonePresenter.printSurroundFlyZones()
    }

    fun initFlyZone() {
        // FlyZoneの初期化
        setUpFlyZone()
        flyZonePresenter.initFlyZone()
    }

    private fun updateFlyZones(flyZones: ArrayList<FlyZoneInformation?>) {
        weakActivityReference.get()?.let { activity ->
            activity.runOnUiThread {
                gMap?.let { map ->
                    updateFlyZonesCore(map, flyZones)
                }
            }
        }
    }

    private fun updateFlyZonesCore(map: GoogleMap, flyZones: ArrayList<FlyZoneInformation?>) {
        map.clear()
        updateDroneLocation()
        for (flyZone in flyZones) {
            flyZone?.subFlyZones?.let { polygonItems ->
                val itemSize = polygonItems.size
                for (i in 0 until itemSize) {
                    if (polygonItems[i].shape == SubFlyZoneShape.POLYGON) {
                        DJILog.d(
                            "updateFlyZonesOnTheMap",
                            "sub polygon points $i size: ${polygonItems[i].vertices.size}"
                        )
                        DJILog.d(
                            "updateFlyZonesOnTheMap",
                            "sub polygon points $i category: ${flyZone.category.value()}"
                        )
                        DJILog.d(
                            "updateFlyZonesOnTheMap",
                            "sub polygon points $i limit height: ${polygonItems[i].maxFlightHeight}"
                        )
                        addPolygonMarker(
                            map,
                            polygonItems[i].vertices,
                            flyZone.category.value(),
                            polygonItems[i].maxFlightHeight
                        )
                    } else if (polygonItems[i].shape == SubFlyZoneShape.CYLINDER) {
                        val tmpPos: LocationCoordinate2D = polygonItems[i].center
                        val latLng = LatLng(tmpPos.latitude, tmpPos.longitude)
                        val subRadius = polygonItems[i].radius
                        DJILog.d(
                            "updateFlyZonesOnTheMap",
                            "sub circle points $i coordinate: ${tmpPos.latitude}, ${tmpPos.longitude}"
                        )
                        DJILog.d(
                            "updateFlyZonesOnTheMap",
                            "sub circle points $i radius: $subRadius"
                        )
                        addCircle(latLng, subRadius, map, flyZone)
                    }
                }
            } ?: run {
                //print polygon
                flyZone?.let {
                    val pos = LatLng(it.coordinate.latitude, it.coordinate.longitude)
                    val radius = it.radius
                    addCircle(pos, radius, map, flyZone)
                }
            }
        }
    }

    private fun addCircle(pos: LatLng, radius: Double, map: GoogleMap, flyZone: FlyZoneInformation) {
        val circle = CircleOptions()
        circle.radius(radius)
        circle.center(pos)
        when (flyZone.category) {
            FlyZoneCategory.WARNING -> circle.strokeColor(Color.GREEN)
            FlyZoneCategory.ENHANCED_WARNING -> circle.strokeColor(Color.BLUE)
            FlyZoneCategory.AUTHORIZATION -> {
                circle.strokeColor(Color.YELLOW)
                unlockableIds.add(flyZone.flyZoneID)
            }
            FlyZoneCategory.RESTRICTED -> circle.strokeColor(Color.RED)
            else -> Unit
        }
        map.addCircle(circle)
    }

    private fun addPolygonMarker(
        map: GoogleMap,
        polygonPoints: List<LocationCoordinate2D>,
        areaLevel: Int,
        height: Int
    ) {
        val points: ArrayList<LatLng> = ArrayList()
        for (point in polygonPoints) {
            points.add(LatLng(point.latitude, point.longitude))
        }
        var fillColor: Int = limitFillColor
        val heightColor = painter.getHeightToColor()[height]
        if (heightColor != null) {
            fillColor = heightColor
        }
        else if (areaLevel == FlyForbidProtocol.LevelType.CAN_UNLIMIT.value()) {
            fillColor = limitCanUnLimitFillColor
        }
        else if (
            areaLevel == FlyForbidProtocol.LevelType.STRONG_WARNING.value() ||
            areaLevel == FlyForbidProtocol.LevelType.WARNING.value()
        ) {
            fillColor = warningFillColor
        }
        map.addPolygon(PolygonOptions()
            .addAll(points)
            .strokeColor(painter.colorTransparent)
            .fillColor(fillColor)
        )
    }

    class FlyFrbBasePainter {
        private val heightToColor: MutableMap<Int, Int> = HashMap()

        @get:ColorInt
        @ColorInt
        val colorTransparent = Color.argb(0, 0, 0, 0)

        fun getHeightToColor(): Map<Int, Int> {
            return heightToColor
        }

        init {
            heightToColor[65] = Color.argb(50, 0, 0, 0)
            heightToColor[125] = Color.argb(25, 0, 0, 0)
        }
    }

    fun addUnlockFlyZone(id: Int) {
        flyZonePresenter.addUnlockFlyZone(id)
    }

    fun unlockFlyZones() {
        flyZonePresenter.unlockFlyZones()
    }

    fun getUnlockedFlyZones() {
        flyZonePresenter.getUnlockedFlyZones()
    }
    //endregion
}