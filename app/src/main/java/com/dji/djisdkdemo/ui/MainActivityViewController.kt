package com.dji.djisdkdemo.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.*
import com.dji.djisdkdemo.R
import dji.common.airlink.PhysicalSource
import dji.common.product.Model
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.accessory.widget.rtk.RTKWidget
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget
import dji.ux.beta.core.util.SettingDefinitions
import dji.ux.beta.core.widget.radar.RadarWidget
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget
import java.lang.ref.WeakReference
import dji.ux.beta.core.widget.fpv.FPVWidget

class MainActivityViewController(appCompatActivity: AppCompatActivity) : LifecycleEventObserver {
    companion object {
        const val TAG = "MainActivityViewController"
    }
    private val weakActivityReference = WeakReference(appCompatActivity)

    // for custom UI
    private lateinit var txtStatusMessage: TextView
    private lateinit var txtProduct: TextView

    // for UXSDK Beta v0.5.1
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
    private var fpvWidget: FPVWidget? = null
    private var fpvInteractionWidget: FPVInteractionWidget? = null
    private var secondaryFPVWidget: FPVWidget? = null
    private var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    private var rtkWidget: RTKWidget? = null
    private var simulatorControlWidget: SimulatorControlWidget? = null

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
        //TODO
    }

    fun onLoginSuccess(message: String) {
        showToast(message)
    }

    fun onLoginFailure(message: String) {
        showToast(message)
    }

    fun showToast(message: String) {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                Toast.makeText(appCompatActivity, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, message)
            }
        }
    }

    fun initUI(savedInstanceState: Bundle?) {
        weakActivityReference.get()?.let { activity ->
            radarWidget = activity.findViewById(R.id.widget_radar)
            fpvWidget = activity.findViewById(R.id.widget_fpv)
            fpvInteractionWidget = activity.findViewById(R.id.widget_fpv_interaction)
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
                gpsSignalWidget.stateChangeCallback = activity.findViewById(R.id.widget_rtk)
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

    // ライフサイクルに連動する処理
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                onCreateProcess()
            }
            Lifecycle.Event.ON_RESUME -> {
                onResumeProcess()
            }
            Lifecycle.Event.ON_PAUSE -> {
                onPauseProcess()
            }
            Lifecycle.Event.ON_START -> Unit
            Lifecycle.Event.ON_STOP -> Unit
            Lifecycle.Event.ON_DESTROY -> Unit
            Lifecycle.Event.ON_ANY -> Unit
        }
    }
}