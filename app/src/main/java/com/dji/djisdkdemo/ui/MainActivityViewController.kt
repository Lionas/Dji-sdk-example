package com.dji.djisdkdemo.ui

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.*
import com.dji.djisdkdemo.R
import dji.common.product.Model
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable
import dji.ux.beta.accessory.widget.rtk.RTKWidget
import dji.ux.beta.cameracore.widget.cameracontrols.CameraControlsWidget
import dji.ux.beta.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsMenuIndicatorWidget
import dji.ux.beta.core.extension.hide
import dji.ux.beta.core.extension.toggleVisibility
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget
import dji.ux.beta.core.widget.radar.RadarWidget
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget
import dji.ux.panel.CameraSettingAdvancedPanel
import dji.ux.panel.CameraSettingExposurePanel
import java.lang.ref.WeakReference
import android.widget.Toast
import dji.sdk.codec.DJICodecManager
import dji.sdk.camera.VideoFeeder
import dji.sdk.camera.VideoFeeder.VideoDataListener
import android.view.TextureView
import com.dji.djisdkdemo.interfaces.ActiveTrackMissionViewControllerCallback

/**
 * メインアクティビティのUI処理
 */
class MainActivityViewController(appCompatActivity: AppCompatActivity) : LifecycleEventObserver {
    companion object {
        const val TAG = "MainActivityViewController"
    }
    private val weakActivityReference = WeakReference(appCompatActivity)

    // for custom UI
    private lateinit var txtStatusMessage: TextView
    private lateinit var txtProduct: TextView
    private lateinit var videoSurface: TextureView

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

    private var systemStatusListPanelWidget: SystemStatusListPanelWidget? = null
    private var rtkWidget: RTKWidget? = null
    private var simulatorControlWidget: SimulatorControlWidget? = null

    //region for Camera
    private var cameraControlsWidget: CameraControlsWidget? = null
    private var cameraSettingsMenuIndicatorWidget: CameraSettingsMenuIndicatorWidget? = null
    private var cameraSettingExposurePanel: CameraSettingExposurePanel? = null
    private var cameraSettingAdvancedPanel: CameraSettingAdvancedPanel? = null
    //endregion

    //region for ActiveTrackMission
    private val activeTrackMissionViewControllerCallback = object :
        ActiveTrackMissionViewControllerCallback {
        override fun setResultToToast(message: String) {
            showToast(message)
        }
    }
    private val activeTrackMissionViewController = ActiveTrackMissionViewController(
        appCompatActivity,
        activeTrackMissionViewControllerCallback
    )
    //endregion

    //region for video surface
    private var receivedVideoDataListener: VideoDataListener? = null
    private var codecManager: DJICodecManager? = null
    //endregion

    private val handler = Handler(Looper.getMainLooper())
    private var model: Model? = null

    private fun initCustomUI() {
        weakActivityReference.get()?.let { activity ->
            txtStatusMessage = activity.findViewById(R.id.txt_status_message)
            txtProduct = activity.findViewById(R.id.txt_product)

            // VideoSurface
            initVideoSurface(activity)
            initReceivedVideoDataListener()

            // ActiveTrackMission
            activeTrackMissionViewController.initUI()
        }
    }

    fun setTextViewStatusMessage(message: String) {
        handler.post {
            txtStatusMessage.text = message
        }
    }

    fun setProductModel(model: Model?) {
        this.model = model
        handler.post {
            txtProduct.text = model?.displayName ?: "(none)"
        }
    }

    fun notifyStatusChange() {
        initPreviewer()
    }

    fun onLoginSuccess() {
        activeTrackMissionViewController.onLoginSuccess()
    }

    private fun showToast(message: String) {
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
        }
    }

    private fun initVideoSurface(activity: AppCompatActivity) {
        videoSurface = activity.findViewById(R.id.video_previewer_surface)
        videoSurface.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (codecManager == null) {
                    codecManager = DJICodecManager(activity, surface, width, height)
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) = Unit

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                codecManager?.cleanSurface()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

    private fun initReceivedVideoDataListener() {
        receivedVideoDataListener = VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }
    }

    private fun initPreviewer() {
        weakActivityReference.get()?.let {
            initVideoSurface(it)
            if (model != Model.UNKNOWN_AIRCRAFT) {
                receivedVideoDataListener?.let { listener ->
                    VideoFeeder.getInstance()?.primaryVideoFeed?.addVideoDataListener(listener)
                }
            }
        }
    }

    private fun resetPreviewer() {
        receivedVideoDataListener?.let {
            VideoFeeder.getInstance()?.primaryVideoFeed?.removeVideoDataListener(it)
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

    // ライフサイクルに連動する処理
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                initCustomUI()
                weakActivityReference.get()?.lifecycle?.addObserver(activeTrackMissionViewController)
            }
            Lifecycle.Event.ON_RESUME -> {
                compositeDisposable = CompositeDisposable()
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
                initPreviewer()
            }
            Lifecycle.Event.ON_PAUSE -> {
                compositeDisposable?.let {
                    it.dispose()
                    compositeDisposable = null
                }
                resetPreviewer()
            }
            Lifecycle.Event.ON_START -> Unit
            Lifecycle.Event.ON_STOP -> Unit
            Lifecycle.Event.ON_DESTROY -> {
                resetPreviewer()
            }
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    fun onLowMemory() = Unit
}
