package com.dji.djisdkdemo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.*
import com.dji.djisdkdemo.R
import com.dji.djisdkdemo.interfaces.ActiveTrackMissionPresenterCallback
import com.dji.djisdkdemo.interfaces.ActiveTrackMissionViewControllerCallback
import com.dji.djisdkdemo.presenter.ActiveTrackMissionPresenter
import dji.common.mission.activetrack.ActiveTrackMissionEvent
import dji.common.mission.activetrack.ActiveTrackState
import dji.common.mission.activetrack.ActiveTrackTargetState
import java.lang.ref.WeakReference
import dji.common.mission.activetrack.SubjectSensingState
import java.util.concurrent.ConcurrentHashMap
import android.widget.RelativeLayout

/**
 * アクティブトラックミッションUI操作
 */
class ActiveTrackMissionViewController(
    appCompatActivity: AppCompatActivity,
    private val callback: ActiveTrackMissionViewControllerCallback
) : LifecycleEventObserver {

    companion object {
        const val MOVE_OFFSET = 20
        const val INVALID_INDEX: Int = -1
    }

    private val weakActivityReference = WeakReference(appCompatActivity)

    private lateinit var pushDrawerIb: ImageButton
    private lateinit var pushDrawerSd: SlidingDrawer
    private lateinit var stopBtn: ImageButton
    private lateinit var pushTv: TextView
    private lateinit var bgLayout: RelativeLayout

    //region active track
    private var activeTrackMissionPresenter: ActiveTrackMissionPresenter? = null
    private val targetViewHashMap: ConcurrentHashMap<Int, MultiTrackingView> = ConcurrentHashMap()
    private lateinit var layoutParams: RelativeLayout.LayoutParams
    private lateinit var sendRectIv: ImageView
    private lateinit var trackingImage: ImageView
    private lateinit var confirmBtn: Button
    private lateinit var rejectBtn: Button
    private lateinit var configBtn: Button
    private lateinit var multiTrackingSwitch: SwitchCompat
    private lateinit var pushBackSwitch: SwitchCompat
    private lateinit var gestureSwitch: SwitchCompat
    private var downX = 0f
    private var downY = 0f
    private var isDrawingRect = false
    private var trackingIndex: Int = INVALID_INDEX
    //endregion

    @SuppressLint("ClickableViewAccessibility")
    // UIの初期化
    fun initUI() {
        weakActivityReference.get()?.let {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            pushDrawerSd = it.findViewById(R.id.tracking_drawer_sd) as SlidingDrawer
            pushTv = it.findViewById(R.id.pointing_push_tv) as TextView
            sendRectIv = it.findViewById(R.id.tracking_send_rect_iv) as ImageView
            trackingImage = it.findViewById(R.id.tracking_rst_rect_iv) as ImageView

            initMultiTrackingSwitch()
            initPushBackSwitch()
            initGestureSwitch()
            initTrackingBgLayout()
            initConfirmButton()
            initStopButton()
            initRejectButton()
            initConfigButton()
            initPushDrawerIb()
        }
    }

    private fun initMultiTrackingSwitch() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                multiTrackingSwitch =
                    appCompatActivity.findViewById(R.id.tracking_multi_tracking_enabled) as SwitchCompat
                multiTrackingSwitch.isChecked = false
                multiTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
                    activeTrackMissionPresenter?.setMultiTrackingEnabled(isChecked)
                }
            }
        }
    }

    private fun initPushBackSwitch() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                pushBackSwitch =
                    appCompatActivity.findViewById(R.id.tracking_pull_back_tb) as SwitchCompat
                pushBackSwitch.isChecked = false
                pushBackSwitch.setOnCheckedChangeListener { _, isChecked ->
                    activeTrackMissionPresenter?.setRetreatEnabled(isChecked)
                }
            }
        }
    }

    private fun initGestureSwitch() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                gestureSwitch =
                    appCompatActivity.findViewById(R.id.tracking_in_gesture_mode) as SwitchCompat
                gestureSwitch.isChecked = false
                gestureSwitch.setOnCheckedChangeListener { _, isChecked ->
                    activeTrackMissionPresenter?.setGestureModeEnabled(isChecked)
                }
            }
        }
    }

    private fun initConfirmButton() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                confirmBtn = appCompatActivity.findViewById(R.id.confirm_btn) as Button
                confirmBtn.setOnClickListener {
                    activeTrackMissionPresenter?.let { presenter ->
                        val isAutoTracking = presenter.isAutoTracking()
                        if (isAutoTracking) {
                            presenter.startAutoSensingMission(trackingIndex)
                        } else {
                            presenter.acceptConfirmation()
                        }
                        stopBtn.visibility = View.VISIBLE
                        rejectBtn.visibility = View.VISIBLE
                        confirmBtn.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTrackingBgLayout() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                bgLayout = appCompatActivity.findViewById(R.id.tracking_bg_layout) as RelativeLayout
                bgLayout.setOnTouchListener { v, event ->
                    onTrackingBgLayoutTouch(v, event)
                }
            }
        }
    }

    private fun initStopButton() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                stopBtn = appCompatActivity.findViewById(R.id.tracking_stop_btn) as ImageButton
                stopBtn.setOnClickListener {
                    trackingIndex = INVALID_INDEX
                    activeTrackMissionPresenter?.stopTracking()
                    trackingImage.let {
                        it.visibility = View.INVISIBLE
                        sendRectIv.visibility = View.INVISIBLE
                        stopBtn.visibility = View.INVISIBLE
                        rejectBtn.visibility = View.INVISIBLE
                        confirmBtn.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun initRejectButton() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                rejectBtn = appCompatActivity.findViewById(R.id.reject_btn) as Button
                rejectBtn.setOnClickListener {
                    activeTrackMissionPresenter?.rejectConfirmation()
                    stopBtn.visibility = View.VISIBLE
                    it.visibility = View.VISIBLE
                    confirmBtn.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun initConfigButton() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                configBtn =
                    appCompatActivity.findViewById(R.id.recommended_configuration_btn) as Button
                configBtn.setOnClickListener {
                    activeTrackMissionPresenter?.setRecommendedConfiguration()
                    it.visibility = View.GONE
                }
            }
        }
    }

    private fun initPushDrawerIb() {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                pushDrawerIb = appCompatActivity.findViewById(R.id.tracking_drawer_control_ib) as ImageButton
                pushDrawerIb.setOnClickListener {
                    if (pushDrawerSd.isOpened) {
                        pushDrawerSd.animateClose();
                    } else {
                        pushDrawerSd.animateOpen();
                    }
                }
            }
        }
    }

    // トラッキング波形をタップした時の処理
    private fun onTrackingBgLayoutTouch(v: View, event: MotionEvent) : Boolean {
        when (event.action) {
            // 指が置かれた
            MotionEvent.ACTION_DOWN -> {
                isDrawingRect = false
                // 指を置いた座標を記録
                downX = event.x
                downY = event.y
            }

            // 指を動かしている（ドラッグ）
            MotionEvent.ACTION_MOVE -> {
                activeTrackMissionPresenter?.let { it ->
                    // 最初に指が置かれた座標と現在の座標の間のマンハッタン距離を算出する
                    val distance = it.calcManhattanDistance(downX, downY, event.x, event.y)
                    // 所定の距離以内で、かつ、領域が描画中でいない場合
                    if (distance < MOVE_OFFSET && isDrawingRect.not()) {
                        // ターゲットの中からタップした座標にあるビューを取得し、ビューの背景を赤色に設定する
                        trackingIndex = it.getTrackingIndex(downX, downY, targetViewHashMap)
                        targetViewHashMap[trackingIndex]?.setBackgroundColor(Color.RED)
                        return true
                    }
                    // 領域を描画中にする
                    isDrawingRect = true
                    // 描画領域を表示する
                    sendRectIv.visibility = View.VISIBLE
                    // 最初にタップした座標と現在の座標までの矩形領域を生成する
                    setSendRectIvPosition(event.x, event.y)
                    // 生成した矩形領域を表示する
                    sendRectIv.requestLayout()
                }
            }
            MotionEvent.ACTION_UP ->
                // 指を離した
                when {
                    gestureSwitch.isChecked -> {
                        // ジェスチャーモードが有効な場合、タップは無効なためメッセージを表示する
                        callback.setResultToToast("Please try to start Gesture Mode!")
                    }
                    isDrawingRect.not() -> {
                        // ドラッグしなかった場合
                        targetViewHashMap[trackingIndex]?.let {
                            val message = "Selected Index: ${trackingIndex}, Please Confirm it!"
                            callback.setResultToToast(message)
                            it.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                    else -> {
                        // 指定した領域のトラッキングを開始する
                        activeTrackMissionPresenter?.let {
                            val rectF: RectF = it.getActiveTrackRect(sendRectIv)
                            it.startTracking(rectF)
                            sendRectIv.visibility = View.INVISIBLE
                            clearCurrentViews()
                        }
                    }
                }
            else -> Unit
        }
        return true
    }

    // トラッキング対象を決めるための矩形領域を生成する
    private fun setSendRectIvPosition(x: Float, y: Float) {
        val l = (if (downX < x) downX else x).toInt()
        val t = (if (downY < y) downY else y).toInt()
        val r = (if (downX >= x) downX else x).toInt()
        val b = (if (downY >= y) downY else y).toInt()
        sendRectIv.x = l.toFloat()
        sendRectIv.y = t.toFloat()
        sendRectIv.layoutParams.width = r - l
        sendRectIv.layoutParams.height = b - t
    }

    // アクティブトラック領域の更新
    private fun updateActiveTrackRect(view: ImageView?, event: ActiveTrackMissionEvent?) {
        view?.let { imageView ->
            event?.let { evt ->
                evt.trackingState?.let { tracking ->
                    tracking.autoSensedSubjects?.let { subjects ->
                        // マルチトラッキングの対象の場合
                        val targetSensingInformation: Array<SubjectSensingState> = subjects
                        weakActivityReference.get().let { appCompatActivity ->
                            appCompatActivity?.runOnUiThread {
                                // 複数の領域を更新する
                                updateMultiTrackingView(appCompatActivity, targetSensingInformation)
                            }
                        }
                    } ?: run {
                        // マルチトラッキングではない場合
                        tracking.targetRect?.let { rectF ->
                            tracking.state?.let { state ->
                                // 単一領域を更新する
                                postResultRect(imageView, rectF, state)
                            }
                        }
                    }
                }
            }
        }
    }

    // 単数または複数のターゲットをビューに反映する
    private fun updateMultiTrackingView(context: Context, targets: Array<SubjectSensingState>) {
        // ターゲットの情報を更新する。新しいターゲットの場合は追加する。
        val indexes: ArrayList<Int> = ArrayList()
        for (target in targets) {
            indexes.add(target.index)
            if (targetViewHashMap.containsKey(target.index)) {
                targetViewHashMap[target.index]?.let {
                    postMultiResultRect(it, target.targetRect, target)
                }
            } else {
                val trackingView = MultiTrackingView(context)
                bgLayout.addView(trackingView, layoutParams)
                targetViewHashMap[target.index] = trackingView
            }
        }

        // いなくなったターゲットをビューから消す
        val missingIndexes: ArrayList<Int> = ArrayList()
        for (key in targetViewHashMap.keys()) {
            var isDisappeared = true
            for (index in indexes) {
                if (index == key) {
                    isDisappeared = false
                    break
                }
            }
            if (isDisappeared) {
                missingIndexes.add(key)
            }
        }
        for (index in missingIndexes) {
            targetViewHashMap.remove(index)?.let {
                bgLayout.removeView(it)
            }
        }
    }

    // RectF座標系からビュー座標系への変換
    private fun translateFromRectToView(view: View, rectF: RectF) {
        val parent = view.parent as View
        val l = ((rectF.centerX() - rectF.width() / 2) * parent.width).toInt()
        val t = ((rectF.centerY() - rectF.height() / 2) * parent.height).toInt()
        val r = ((rectF.centerX() + rectF.width() / 2) * parent.width).toInt()
        val b = ((rectF.centerY() + rectF.height() / 2) * parent.height).toInt()
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                view.x = l.toFloat()
                view.y = t.toFloat()
                view.layoutParams.width = r - l
                view.layoutParams.height = b - t
                view.requestLayout()
            }
        }
    }

    // マルチトラッキングビューの描画
    private fun postMultiResultRect(v: MultiTrackingView, rectF: RectF, info: SubjectSensingState) {
        // RectF座標系からビュー座標系への変換
        translateFromRectToView(v, rectF)

        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                trackingImage.visibility = View.INVISIBLE
                v.updateView(info)
            }
        }
    }

    // アクティブトラックの状態をビュー領域に反映する
    private fun postResultRect(iv: ImageView, rectF: RectF, targetState: ActiveTrackTargetState) {
        // RectF座標系からビュー座標系への変換
        translateFromRectToView(iv, rectF)

        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                when (targetState) {
                    ActiveTrackTargetState.CANNOT_CONFIRM,
                    ActiveTrackTargetState.UNKNOWN ->
                        iv.setImageResource(R.drawable.visual_track_cannotconfirm)
                    ActiveTrackTargetState.WAITING_FOR_CONFIRMATION ->
                        iv.setImageResource(R.drawable.visual_track_needconfirm)
                    ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE ->
                        iv.setImageResource(R.drawable.visual_track_lowconfidence)
                    ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE ->
                        iv.setImageResource(R.drawable.visual_track_highconfidence)
                    else -> Unit
                }
                trackingImage.visibility = View.VISIBLE
            }
        }
    }

    // アクティブトラックで使用する各種ボタンの表示と動作を設定する
    private fun updateButtonsVisibilityAndClickable(event: ActiveTrackMissionEvent) {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                when (event.currentState) {
                    ActiveTrackState.AUTO_SENSING,
                    ActiveTrackState.AUTO_SENSING_FOR_QUICK_SHOT,
                    ActiveTrackState.WAITING_FOR_CONFIRMATION -> {
                        stopBtn.visibility = View.VISIBLE
                        stopBtn.isClickable = true
                        confirmBtn.visibility = View.VISIBLE
                        confirmBtn.isClickable = true
                        rejectBtn.visibility = View.VISIBLE
                        rejectBtn.isClickable = true
                        configBtn.visibility = View.GONE
                    }

                    ActiveTrackState.AIRCRAFT_FOLLOWING,
                    ActiveTrackState.ONLY_CAMERA_FOLLOWING,
                    ActiveTrackState.FINDING_TRACKED_TARGET,
                    ActiveTrackState.CANNOT_CONFIRM,
                    ActiveTrackState.PERFORMING_QUICK_SHOT -> {
                        stopBtn.visibility = View.VISIBLE
                        stopBtn.isClickable = true
                        confirmBtn.visibility = View.INVISIBLE
                        confirmBtn.isClickable = false
                        rejectBtn.visibility = View.VISIBLE
                        rejectBtn.isClickable = true
                        configBtn.visibility = View.GONE
                    }

                    else -> {
                        stopBtn.visibility = View.INVISIBLE
                        stopBtn.isClickable = true
                        confirmBtn.visibility = View.INVISIBLE
                        confirmBtn.isClickable = false
                        rejectBtn.visibility = View.INVISIBLE
                        rejectBtn.isClickable = true
                        trackingImage.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    // ライフサイクルに連動する処理
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // ActiveTrackMissionPresenterの生成とコールバックの定義
                activeTrackMissionPresenter = ActiveTrackMissionPresenter(
                    object: ActiveTrackMissionPresenterCallback {
                        override fun setResultToToast(message: String) {
                            callback.setResultToToast(message)
                        }

                        override fun setMultiTracking(enabled: Boolean) {
                            multiTrackingSwitch.apply {
                                isChecked = enabled
                            }
                        }

                        override fun disableButtonVisibilities() {
                            weakActivityReference.get().let { appCompatActivity ->
                                appCompatActivity?.runOnUiThread {
                                    confirmBtn.visibility = View.INVISIBLE
                                    stopBtn.visibility = View.INVISIBLE
                                    rejectBtn.visibility = View.INVISIBLE
                                    configBtn.visibility = View.VISIBLE
                                }
                            }
                        }

                        override fun setPushBackSwitch(enabled: Boolean) {
                            pushBackSwitch.apply {
                                isChecked = enabled
                            }
                        }

                        override fun setGestureSwitch(enabled: Boolean) {
                            gestureSwitch.apply {
                                isChecked = enabled
                            }
                        }

                        override fun setResultToActiveTrackStatus(status: String) {
                            weakActivityReference.get().let { appCompatActivity ->
                                appCompatActivity?.runOnUiThread {
                                    pushTv.text = status
                                }
                            }
                        }

                        override fun updateActiveTrackRect(event: ActiveTrackMissionEvent) {
                            updateActiveTrackRect(trackingImage, event)
                        }

                        override fun updateButtonVisibility(event: ActiveTrackMissionEvent) {
                            updateButtonsVisibilityAndClickable(event)
                        }

                        override fun clearCurrentView() {
                            clearCurrentViews()
                        }

                        override fun setDrawingRect(isDrawing: Boolean) {
                            isDrawingRect = isDrawing
                        }

                        override fun setTrackingIndex(index: Int) {
                            trackingIndex = index
                        }
                    }
                )

                // ActiveTrackMissionの初期化
                activeTrackMissionPresenter?.initActiveTrackMission()
            }
            Lifecycle.Event.ON_RESUME -> {
                // 現在の状態の取得と反映
                activeTrackMissionPresenter?.getGestureModeEnabled()?.let {
                    gestureSwitch.isChecked = it
                }
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    activeTrackMissionPresenter?.getRetreatEnabled()
                }, 1000L)
            }
            Lifecycle.Event.ON_PAUSE -> Unit
            Lifecycle.Event.ON_START -> Unit
            Lifecycle.Event.ON_STOP -> Unit
            Lifecycle.Event.ON_DESTROY -> {
                activeTrackMissionPresenter?.destroy()
            }
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    private fun clearCurrentViews() {
        if (targetViewHashMap.isNotEmpty()) {
            val it: MutableIterator<Map.Entry<Int, MultiTrackingView>> = targetViewHashMap.entries.iterator()
            while (it.hasNext()) {
                val entry: Map.Entry<Int, MultiTrackingView> = it.next()
                val view: MultiTrackingView = entry.value
                it.remove()
                weakActivityReference.get().let { appCompatActivity ->
                    appCompatActivity?.runOnUiThread {
                        bgLayout.removeView(view)
                    }
                }
            }
        }
    }
}
