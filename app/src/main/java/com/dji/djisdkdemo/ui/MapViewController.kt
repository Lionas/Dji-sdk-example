package com.dji.djisdkdemo.ui

import androidx.appcompat.app.AppCompatActivity
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.interfaces.MapViewControllerCallback
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class MapViewController(
    appCompatActivity: AppCompatActivity?,
    private val callback: MapViewControllerCallback
) :
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener
{
    private val weakActivityReference = WeakReference(appCompatActivity)

    private var gMap: GoogleMap? = null
    private var droneLocationLat: Double = 0.0 // init value
    private var droneLocationLng: Double = 0.0 // init value
    private var droneMarker: Marker? = null
    private val cameraZoomLevel = 16.0f
    private val initMapPosition = LatLng(35.473969,140.222304) // Chiba, Japan

    // 初めて正しいGPSを受信したかどうか
    private var isFirstReceiveCorrectGps = false

    companion object {
        // TODO インスタンス化も検討する
        fun checkGpsCoordinates(latitude: Double, longitude: Double): Boolean {
            return latitude > -90 && latitude < 90 &&
                    longitude > -180 && longitude < 180 &&
                    latitude != 0.0 && longitude != 0.0
        }
    }

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
        gMap?.let {
            if (isAddWaypointMode){
                // add waypoint
                markWaypoint(latlng)
                callback.addWaypoint(latlng)
            } else {
                callback.onMapClick()
            }
        }
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
        val droneIcon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_drone)
        markerOptions.icon(droneIcon)
        return markerOptions
    }

    fun cameraUpdate() {
        val pos = LatLng(droneLocationLat, droneLocationLng)
        val cu: CameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, cameraZoomLevel)
        gMap?.moveCamera(cu)
    }

    //region Waypoint
    private var isAddWaypointMode: Boolean = false

    fun flipAddWaypointMode() {
        isAddWaypointMode = isAddWaypointMode.not()
    }

    fun isAddWaypointMode(): Boolean {
        return isAddWaypointMode
    }

    private var waypointMarkers: MutableMap<Int, Marker> = ConcurrentHashMap<Int, Marker>()

    private fun markWaypoint(point: LatLng) {
        val markerOptions = MarkerOptions()
        markerOptions.position(point)
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        val marker = gMap?.addMarker(markerOptions)
        marker?.let {
            waypointMarkers.apply {
                this[this.size] = it
            }
        }
    }

    fun clearMap() {
        gMap?.clear()
        waypointMarkers.clear()

    }
    //endregion
}