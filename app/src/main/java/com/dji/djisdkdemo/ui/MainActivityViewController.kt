package com.dji.djisdkdemo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.activity.MainActivity
import com.dji.mapkit.core.maps.DJIMap
import com.dji.mapkit.core.models.DJILatLng
import dji.common.airlink.PhysicalSource
import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.common.product.Model
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.accessory.widget.rtk.RTKWidget
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.widget.radar.RadarWidget
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget
import dji.ux.beta.map.widget.map.MapWidget
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget

class MainActivityViewController(private val activity: AppCompatActivity) {

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
    private var mapWidget: MapWidget? = null
    private var secondaryFPVWidget: dji.ux.beta.core.widget.fpv.FPVWidget? = null
    private var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    private var rtkWidget: RTKWidget? = null
    private var simulatorControlWidget: SimulatorControlWidget? = null

    private val handler = Handler(Looper.getMainLooper())

    fun initUI() {
        // Initialize UI
        txtStatusMessage = activity.findViewById(R.id.txt_status_message)
        txtProduct = activity.findViewById(R.id.txt_product)
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
        activity.sendBroadcast(intent)
    }

    fun initUxSdkUI(savedInstanceState: Bundle?) {
        radarWidget = activity.findViewById(R.id.widget_radar)
        fpvWidget = activity.findViewById(R.id.widget_fpv)
        fpvWidget?.setOnClickListener {
            onViewClick(fpvWidget)
        }
        fpvInteractionWidget = activity.findViewById(R.id.widget_fpv_interaction)
        mapWidget = activity.findViewById(R.id.widget_map)
        secondaryFPVWidget = activity.findViewById(R.id.widget_secondary_fpv)
        secondaryFPVWidget?.setOnClickListener {
            swapVideoSource()
        }
        systemStatusListPanelWidget = activity.findViewById(R.id.widget_panel_system_status_list)
        rtkWidget = activity.findViewById(R.id.widget_rtk)
        simulatorControlWidget = activity.findViewById(R.id.widget_simulator_control)

        parentView = activity.findViewById(R.id.root_view)

        widgetHeight = activity.resources.getDimension(R.dimen.mini_map_height).toInt()
        widgetWidth = activity.resources.getDimension(R.dimen.mini_map_width).toInt()
        widgetMargin = activity.resources.getDimension(R.dimen.mini_map_margin).toInt()

        val displayMetrics = activity.resources.displayMetrics
        deviceHeight = displayMetrics.heightPixels
        deviceWidth = displayMetrics.widthPixels

        setM200SeriesWarningLevelRanges()
        mapWidget?.let {
            it.initAMap { map: DJIMap ->
                map.setOnMapClickListener { latLng: DJILatLng? ->
                    onViewClick(it)
                }
                map.uiSettings.setZoomControlsEnabled(false)
            }
            it.userAccountLoginWidget.visibility = View.GONE
            it.onCreate(savedInstanceState)
        }

        // Setup top bar state callbacks
        topBarPanelWidget = activity.findViewById<TopBarPanelWidget>(R.id.panel_top_bar)
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

        mapWidget?.let {
            userAccountLoginWidget = it.userAccountLoginWidget
        }
        userAccountLoginWidget?.let {
            val params = it.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = deviceHeight / 10 + DisplayUtil.dipToPx(activity, 10f).toInt()
            it.layoutParams = params
        }

        // 各種UIカスタム
        customTopBarPanel()
    }

    private fun customTopBarPanel() {
        val topBarPanelCustom = TopBarPanelCustom(topBarPanelWidget)
        val colorInt = activity.getColor(R.color.gray)
        topBarPanelCustom.setSystemStatusMessageTextColor(colorInt)

        val drawable = activity.getDrawable(R.drawable.fpv_gradient_left)
        topBarPanelCustom.setSystemStatusBackgroundDrawable(drawable)
    }

    fun onResume() {
        mapWidget?.onResume()
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
        mapWidget?.onSaveInstanceState(outState)
    }

    fun onLowMemory() {
        mapWidget?.onLowMemory()
    }

    fun onPause() {
        compositeDisposable?.let {
            it.dispose()
            compositeDisposable = null
        }
        mapWidget?.onPause()
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
            simulatorControlWidget
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
            parentView?.addView(fpvWidget, 0)

            //resize widgets
            resizeViews(fpvWidget, mapWidget)
            //enable interaction on FPV
            fpvInteractionWidget?.isInteractionEnabled = true
            //disable user login widget on map
            userAccountLoginWidget?.visibility = View.GONE
            isMapMini = true
        } else if (view === mapWidget && isMapMini) {
            // reorder widgets
            parentView?.let {
                it.removeView(fpvWidget)
                it.addView(fpvWidget, it.indexOfChild(mapWidget) + 1)
                //resize widgets
                resizeViews(mapWidget, fpvWidget)

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
        val enlargeAnimation: ResizeAnimation =
            ResizeAnimation(
                viewToEnlarge,
                widgetWidth,
                widgetHeight,
                deviceWidth,
                deviceHeight,
                0
            )
        viewToEnlarge?.startAnimation(enlargeAnimation)

        //shrink second widget
        val shrinkAnimation: ResizeAnimation =
            ResizeAnimation(
                viewToShrink,
                deviceWidth,
                deviceHeight,
                widgetWidth,
                widgetHeight,
                widgetMargin
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
}