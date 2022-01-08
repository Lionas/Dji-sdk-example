package com.dji.djisdkdemo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.*
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.activity.MainActivity
import com.dji.djisdkdemo.interfaces.MainActivityViewControllerCallback
import dji.common.airlink.PhysicalSource
import dji.common.product.Model
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.accessory.widget.rtk.RTKWidget
import dji.ux.beta.cameracore.widget.cameracontrols.CameraControlsWidget
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsMenuIndicatorWidget
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.extension.toggleVisibility
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.widget.radar.RadarWidget
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget
import dji.ux.panel.CameraSettingAdvancedPanel
import dji.ux.panel.CameraSettingExposurePanel
import java.lang.ref.WeakReference
import com.google.android.gms.maps.SupportMapFragment

import dji.common.mission.waypoint.WaypointMissionHeadingMode
import android.widget.RadioGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.NestedScrollView
import com.dji.djisdkdemo.presenter.WayPointMissionPresenter
import com.google.android.gms.maps.model.LatLng
import dji.common.mission.waypoint.WaypointMissionFinishedAction

class MainActivityViewController(appCompatActivity: AppCompatActivity) : LifecycleEventObserver {
    companion object {
        const val TAG = "MainActivityViewController"
    }
    private val weakActivityReference = WeakReference(appCompatActivity)

    // for custom UI
    private lateinit var txtStatusMessage: TextView
    private lateinit var txtProduct: TextView

    // for UXSDK Beta v0.5.1
    private var isMapMini = true
    private var widgetHeight = 0
    private var widgetWidth = 0
    private var widgetMargin = 0
    private var deviceWidth = 0
    private var deviceHeight = 0
    private var compositeDisposable: CompositeDisposable? = null

    private var userAccountLoginWidget: UserAccountLoginWidget? = null
    private lateinit var topBarPanelWidget: TopBarPanelWidget
    private var parentView: ConstraintLayout? = null
    private var radarWidget: RadarWidget? = null
    private var fpvWidget: dji.ux.beta.core.widget.fpv.FPVWidget? = null
    private var fpvInteractionWidget: FPVInteractionWidget? = null

    //region map
    //    private var mapWidget: MapWidget? = null
    private var mapFragmentContainerView: FragmentContainerView? = null
    private var mapFragment: SupportMapFragment? = null

    // callback from map view controller
    private val callback = object: MainActivityViewControllerCallback {
        override fun onMapClick() {
            onViewClick(mapFragmentContainerView)
        }

        // Waypoint
        override fun addWaypoint(latLng: LatLng) {
            wayPointMissionPresenter.addWaypoint(latLng)
        }
    }

    private val mapViewController: MapViewController =
        MapViewController(weakActivityReference.get(), callback)
    //endregion

    private var secondaryFPVWidget: dji.ux.beta.core.widget.fpv.FPVWidget? = null
    private var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    private var rtkWidget: RTKWidget? = null
    private var simulatorControlWidget: SimulatorControlWidget? = null

    //region for Camera
    private var cameraControlsWidget: CameraControlsWidget? = null
    private var cameraSettingsMenuIndicatorWidget: CameraSettingsMenuIndicatorWidget? = null
    private var cameraSettingExposurePanel: CameraSettingExposurePanel? = null
    private var cameraSettingAdvancedPanel: CameraSettingAdvancedPanel? = null
    //endregion

    private val handler = Handler(Looper.getMainLooper())

    private fun initCustomUI() {
        weakActivityReference.get()?.let { activity ->
            txtStatusMessage = activity.findViewById(R.id.txt_status_message)
            txtProduct = activity.findViewById(R.id.txt_product)
        }
    }

    fun setTextViewStatusMessage(message: String) {
        handler.post {
            txtStatusMessage.text = message
        }
    }

    fun setTextViewProduct(name: String) {
        handler.post {
            txtProduct.text = name
        }
    }

    fun notifyStatusChange() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, 500)
    }

    private val updateRunnable = Runnable {
        val intent = Intent(MainActivity.FLAG_CONNECTION_CHANGE)
        weakActivityReference.get()?.sendBroadcast(intent)
    }

    fun initUI(savedInstanceState: Bundle?) {
        weakActivityReference.get()?.let { activity ->
            radarWidget = activity.findViewById(R.id.widget_radar)
            fpvWidget = activity.findViewById(R.id.widget_fpv)
            fpvWidget?.setOnClickListener {
                onViewClick(fpvWidget)
            }
            fpvInteractionWidget = activity.findViewById(R.id.widget_fpv_interaction)
            mapFragmentContainerView = activity.findViewById(R.id.widget_map)
            secondaryFPVWidget = activity.findViewById(R.id.widget_secondary_fpv)
            secondaryFPVWidget?.setOnClickListener {
                swapVideoSource()
            }
            systemStatusListPanelWidget = activity.findViewById(R.id.widget_panel_system_status_list)
            rtkWidget = activity.findViewById(R.id.widget_rtk)
            simulatorControlWidget = activity.findViewById(R.id.widget_simulator_control)
            userAccountLoginWidget = activity.findViewById(R.id.widget_user_login)
            parentView = activity.findViewById(R.id.root_view)

            widgetHeight = activity.resources.getDimension(R.dimen.mini_map_height).toInt()
            widgetWidth = activity.resources.getDimension(R.dimen.mini_map_width).toInt()
            widgetMargin = activity.resources.getDimension(R.dimen.mini_map_margin).toInt()

            val displayMetrics = activity.resources.displayMetrics
            deviceHeight = displayMetrics.heightPixels
            deviceWidth = displayMetrics.widthPixels

            setM200SeriesWarningLevelRanges()

            mapFragmentContainerView?.let {
                mapFragment = activity.supportFragmentManager.findFragmentById(R.id.widget_map) as SupportMapFragment
                mapFragment?.getMapAsync(mapViewController)
            }

            userAccountLoginWidget?.visibility = View.GONE

            // Setup top bar state callbacks
            topBarPanelWidget = activity.findViewById(R.id.panel_top_bar)
            val systemStatusWidget = topBarPanelWidget.systemStatusWidget
            if (systemStatusWidget != null) {
                systemStatusWidget.stateChangeCallback =
                    activity.findViewById(R.id.widget_panel_system_status_list)
            }

            val simulatorIndicatorWidget = topBarPanelWidget.simulatorIndicatorWidget
            if (simulatorIndicatorWidget != null) {
                simulatorIndicatorWidget.stateChangeCallback =
                    activity.findViewById(R.id.widget_simulator_control)
            }

            val gpsSignalWidget = topBarPanelWidget.gpsSignalWidget
            if (gpsSignalWidget != null) {
                gpsSignalWidget.stateChangeCallback =
                    activity.findViewById(R.id.widget_rtk)
            }

            // Camera Settings
            cameraControlsWidget = activity.findViewById(R.id.camera_controls_widget)
            cameraSettingsMenuIndicatorWidget = cameraControlsWidget?.cameraSettingsMenuIndicatorWidget
            cameraSettingExposurePanel = activity.findViewById(R.id.camera_setting_exposure_panel)
            // MENUをシングルタップでExposureの設定画面へ
            cameraSettingsMenuIndicatorWidget?.setOnClickListener {
                cameraSettingExposurePanel?.toggleVisibility()
                hideOtherPanels(cameraSettingExposurePanel)
            }
            cameraSettingAdvancedPanel = activity.findViewById(R.id.camera_setting_advenced_panel)
            // MENUをロングタップでその他のカメラの設定へ
            cameraSettingsMenuIndicatorWidget?.setOnLongClickListener {
                cameraSettingAdvancedPanel?.toggleVisibility()
                hideOtherPanels(cameraSettingAdvancedPanel)
                true
            }

            // Waypoint
            wayPointModeBtn = activity.findViewById(R.id.btn_waypoint_mode)
            wayPointModeBtn.setOnClickListener {
                showWayPointControlDialog()
            }
        }
    }

    private fun onCreateProcess() {
        initCustomUI()
    }

    private fun onResumeProcess() {
        compositeDisposable = CompositeDisposable()
        secondaryFPVWidget?.let {
            compositeDisposable?.add(
                it.cameraName
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { cameraName: String? ->
                        cameraName?.let {
                            this.updateSecondaryVideoVisibility(it)
                        }
                    })
        }
        systemStatusListPanelWidget?.let {
            compositeDisposable?.add(
                it.closeButtonPressed()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { pressed: Boolean ->
                        if (pressed) {
                            systemStatusListPanelWidget?.hide()
                        }
                    })
        }
        rtkWidget?.let {
            compositeDisposable?.add(
                it.getUIStateUpdates()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { uiState: RTKWidget.UIState ->
                        if (uiState is RTKWidget.UIState.VisibilityUpdated) {
                            if (uiState.isVisible) {
                                hideOtherPanels(it)
                            }
                        }
                    }
            )
        }
        simulatorControlWidget?.let {
            compositeDisposable?.add(
                it.getUIStateUpdates()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { simulatorControlWidgetState: SimulatorControlWidget.UIState ->
                        if (simulatorControlWidgetState is SimulatorControlWidget.UIState.VisibilityUpdated) {
                            if (simulatorControlWidgetState.isVisible) {
                                hideOtherPanels(it)
                            }
                        }
                    }
            )
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
    }

    fun onLowMemory() {
        // TODO
//        mapWidget?.onLowMemory()
    }

    private fun onPauseProcess() {
        compositeDisposable?.let {
            it.dispose()
            compositeDisposable = null
        }
    }

    /**
     * Hide the secondary FPV widget when there is no secondary camera.
     *
     * @param cameraName The name of the secondary camera.
     */
    private fun updateSecondaryVideoVisibility(cameraName: String) {
        if (cameraName == PhysicalSource.UNKNOWN.name) {
            secondaryFPVWidget?.visibility = View.GONE
        } else {
            secondaryFPVWidget?.visibility = View.VISIBLE
        }
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private fun swapVideoSource() {
        if (secondaryFPVWidget?.videoSource == SettingDefinitions.VideoSource.SECONDARY) {
            fpvWidget?.videoSource = SettingDefinitions.VideoSource.SECONDARY
            secondaryFPVWidget?.videoSource = SettingDefinitions.VideoSource.PRIMARY
        } else {
            fpvWidget?.videoSource = SettingDefinitions.VideoSource.PRIMARY
            secondaryFPVWidget?.videoSource = SettingDefinitions.VideoSource.SECONDARY
        }
    }

    //region Utils
    private fun hideOtherPanels(widget: View?) {
        val panels = arrayOf<View?>(
            rtkWidget,
            simulatorControlWidget,
            cameraSettingExposurePanel,
            cameraSettingAdvancedPanel
        )
        for (panel in panels) {
            if (widget !== panel) {
                panel?.visibility = View.GONE
            }
        }
    }

    private fun setM200SeriesWarningLevelRanges() {
        val m200SeriesModels = arrayOf(
            Model.MATRICE_200,
            Model.MATRICE_210,
            Model.MATRICE_210_RTK,
            Model.MATRICE_200_V2,
            Model.MATRICE_210_V2,
            Model.MATRICE_210_RTK_V2
        )
        val ranges = floatArrayOf(70f, 30f, 20f, 12f, 6f, 3f)
        radarWidget?.setWarningLevelRanges(m200SeriesModels, ranges)
    }

    /**
     * Swaps the FPV and Map Widgets.
     *
     * @param view The thumbnail view that was clicked.
     */
    private fun onViewClick(view: View?) {
        if (view === fpvWidget && !isMapMini) {
            //reorder widgets
            parentView?.removeView(fpvWidget)
            try {
                parentView?.addView(fpvWidget, 0)
            } catch (e: IllegalStateException) {
                // 連打対応
                Log.e(TAG, e.localizedMessage ?: "IllegalStateException")
                parentView?.postDelayed({
                    parentView?.addView(fpvWidget, 0)
                }, 1000L)
            }

            //resize widgets
            resizeViews(fpvWidget, mapFragmentContainerView)
            //enable interaction on FPV
            fpvInteractionWidget?.isInteractionEnabled = true
            //disable user login widget on map
            userAccountLoginWidget?.visibility = View.GONE
            isMapMini = true

            // update(move) map position
            mapViewController.cameraUpdate()

        } else if (view === mapFragmentContainerView && isMapMini) {
            // reorder widgets
            parentView?.let {
                it.removeView(fpvWidget)
                it.addView(fpvWidget, it.indexOfChild(mapFragmentContainerView) + 1)
                //resize widgets
                resizeViews(mapFragmentContainerView, fpvWidget)
            }
            //disable interaction on FPV
            fpvInteractionWidget?.isInteractionEnabled = false
            //enable user login widget on map
            userAccountLoginWidget?.visibility = View.VISIBLE
            isMapMini = false
        }
    }

    /**
     * Helper method to resize the FPV and Map Widgets.
     *
     * @param viewToEnlarge The view that needs to be enlarged to full screen.
     * @param viewToShrink  The view that needs to be shrunk to a thumbnail.
     */
    private fun resizeViews(viewToEnlarge: View?, viewToShrink: View?) {
        //enlarge first widget
        val enlargeAnimation = ResizeAnimation(
            viewToEnlarge, widgetWidth, widgetHeight, deviceWidth, deviceHeight, 0
        )
        viewToEnlarge?.startAnimation(enlargeAnimation)

        //shrink second widget
        val shrinkAnimation = ResizeAnimation(
            viewToShrink, deviceWidth, deviceHeight, widgetWidth, widgetHeight, widgetMargin
        )
        viewToShrink?.startAnimation(shrinkAnimation)
    }

    /**
     * Animation to change the size of a view.
     */
    private class ResizeAnimation(
        private val view: View?,
        private val fromWidth: Int,
        private val fromHeight: Int,
        private val toWidth: Int,
        private val toHeight: Int,
        private val margin: Int
    ) :
        Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val height = (toHeight - fromHeight) * interpolatedTime + fromHeight
            val width = (toWidth - fromWidth) * interpolatedTime + fromWidth
            view?.let {
                val p = it.layoutParams as ConstraintLayout.LayoutParams
                p.height = height.toInt()
                p.width = width.toInt()
                p.rightMargin = margin
                p.bottomMargin = margin
                it.requestLayout()
            }
        }

        companion object {
            private const val DURATION = 300
        }

        init {
            duration = DURATION.toLong()
        }
    }

    // ライフサイクルに連動する処理
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                onCreateProcess()
                onCreateWayPoint()
            }
            Lifecycle.Event.ON_RESUME -> {
                onResumeProcess()
            }
            Lifecycle.Event.ON_PAUSE -> {
                onPauseProcess()
            }
            Lifecycle.Event.ON_START -> Unit
            Lifecycle.Event.ON_STOP -> Unit
            Lifecycle.Event.ON_DESTROY -> {
                onDestroyWayPoint()
            }
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    //region MapController
    fun updateDroneLocation(lat: Double, lng: Double) {
        mapViewController.setDroneLocation(lat, lng)
        mapViewController.updateDroneLocation()
    }
    //endregion

    //region Waypoint
    private lateinit var wayPointMissionPresenter: WayPointMissionPresenter
    private lateinit var addBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var configBtn: Button
    private lateinit var uploadBtn: Button
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var wayPointModeBtn: AppCompatImageButton

    fun setEnableWayPoint(enable: Boolean) {
        weakActivityReference.get()?.let {
            it.runOnUiThread {
                if (enable) {
                    wayPointModeBtn.visibility = View.VISIBLE
                } else {
                    wayPointModeBtn.visibility = View.GONE
                }
            }
        }
    }

    private fun onCreateWayPoint() {
        wayPointMissionPresenter = WayPointMissionPresenter()
        wayPointMissionPresenter.addListener()
    }

    private fun onDestroyWayPoint() {
        wayPointMissionPresenter.removeListener()
    }

    private fun showWayPointControlDialog() {
        weakActivityReference.get()?.let {
            val wayPointControl = it.layoutInflater
                .inflate(R.layout.dialog_waypoint_control, null) as LinearLayout
            wayPointControl.apply {
                addBtn = findViewById<View>(R.id.add) as Button
                addBtn.setOnClickListener {
                    enableDisableAdd()
                }
                clearBtn = findViewById<View>(R.id.clear) as Button
                clearBtn.setOnClickListener {
                    mapViewController.clearMap()
                }
                configBtn = findViewById<View>(R.id.config) as Button
                configBtn.setOnClickListener {
                    showSettingDialog()
                }
                uploadBtn = findViewById<View>(R.id.upload) as Button
                uploadBtn.setOnClickListener {
                    wayPointMissionPresenter.uploadWayPointMission()
                }
                startBtn = findViewById<View>(R.id.start) as Button
                startBtn.setOnClickListener {
                    wayPointMissionPresenter.startWaypointMission()
                }
                stopBtn = findViewById<View>(R.id.stop) as Button
                stopBtn.setOnClickListener {
                    wayPointMissionPresenter.stopWaypointMission()
                }
            }

            AlertDialog.Builder(ContextThemeWrapper(it, R.style.DialogTheme))
                .setTitle("")
                .setView(wayPointControl)
                .setPositiveButton("Close") { dialog, _ -> dialog.cancel() }
                .create()
                .show()
        }
    }

    private fun enableDisableAdd() {
        weakActivityReference.get()?.let {
            // flip mode
            mapViewController.flipAddWaypointMode()
            // change button label
            val text = if (mapViewController.isAddWaypointMode()) {
                it.resources.getString(R.string.exit)
            } else {
                it.resources.getString(R.string.add)
            }
            addBtn.text = text
        }
    }

    // TODO 設定ずみの数字を取るようにする
    private fun showSettingDialog() {
        weakActivityReference.get()?.let {
            val wayPointSettings =
                it.layoutInflater.inflate(R.layout.dialog_waypoint_setting, null) as NestedScrollView
            wayPointSettings.apply {
                // Altitude
                val wpAltitude = findViewById<View>(R.id.altitude) as TextView

                // Speed
                val speed = findViewById<View>(R.id.speed) as RadioGroup
                speed.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.lowSpeed -> {
                            wayPointMissionPresenter.setSpeed(WayPointMissionPresenter.SPEED.LOW)
                        }
                        R.id.MidSpeed -> {
                            wayPointMissionPresenter.setSpeed(WayPointMissionPresenter.SPEED.MIDDLE)
                        }
                        R.id.HighSpeed -> {
                            wayPointMissionPresenter.setSpeed(WayPointMissionPresenter.SPEED.HIGH)
                        }
                    }
                }

                // Action after finished mission
                val actionAfterFinished =
                    findViewById<View>(R.id.actionAfterFinished) as RadioGroup
                actionAfterFinished.setOnCheckedChangeListener { _, checkedId ->
                    Log.d(TAG, "Select finish action")
                    when (checkedId) {
                        R.id.finishNone -> {
                            wayPointMissionPresenter.setFinishedAction(
                                WaypointMissionFinishedAction.NO_ACTION
                            )
                        }
                        R.id.finishGoHome -> {
                            wayPointMissionPresenter.setFinishedAction(
                                WaypointMissionFinishedAction.GO_HOME
                            )
                        }
                        R.id.finishAutoLanding -> {
                            wayPointMissionPresenter.setFinishedAction(
                                WaypointMissionFinishedAction.AUTO_LAND
                            )
                        }
                        R.id.finishToFirst -> {
                            wayPointMissionPresenter.setFinishedAction(
                                WaypointMissionFinishedAction.GO_FIRST_WAYPOINT
                            )
                        }
                    }
                }

                // Heading
                val heading = findViewById<View>(R.id.heading) as RadioGroup
                heading.setOnCheckedChangeListener { group, checkedId ->
                    Log.d(TAG, "Select heading")
                    when (checkedId) {
                        R.id.headingNext -> {
                            wayPointMissionPresenter.setHeadingMode(
                                WaypointMissionHeadingMode.AUTO
                            )
                        }
                        R.id.headingInitDirec -> {
                            wayPointMissionPresenter.setHeadingMode(
                                WaypointMissionHeadingMode.USING_INITIAL_DIRECTION
                            )
                        }
                        R.id.headingRC -> {
                            wayPointMissionPresenter.setHeadingMode(
                                WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER
                            )
                        }
                        R.id.headingWP -> {
                            wayPointMissionPresenter.setHeadingMode(
                                WaypointMissionHeadingMode.USING_WAYPOINT_HEADING
                            )
                        }
                    }
                }

                // Create Dialog
                AlertDialog.Builder(ContextThemeWrapper(it, R.style.DialogTheme))
                    .setTitle("")
                    .setView(wayPointSettings)
                    .setPositiveButton("Finish") { _, _ ->
                        // set altitude
                        val altitudeString = wpAltitude.text.toString()
                        wayPointMissionPresenter.setAltitude(altitudeString.trim().toIntOrNull())
                        // display setting logs
                        wayPointMissionPresenter.printSettings()
                        // load mission
                        wayPointMissionPresenter.configWayPointMission()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                    .create()
                    .show()
            }
        }
    }
    //endregion
}
