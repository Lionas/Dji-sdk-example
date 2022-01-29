package com.dji.djisdkdemo.presenter

import android.graphics.RectF
import android.view.View
import com.dji.djisdkdemo.interfaces.ActiveTrackMissionPresenterCallback
import com.dji.djisdkdemo.ui.ActiveTrackMissionViewController
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.mission.MissionControl
import dji.sdk.mission.activetrack.ActiveTrackOperator
import dji.keysdk.KeyManager
import dji.keysdk.FlightControllerKey
import dji.keysdk.DJIKey
import kotlin.math.abs
import com.dji.djisdkdemo.ui.MultiTrackingView
import dji.common.mission.activetrack.*
import java.util.concurrent.ConcurrentHashMap

/**
 * アクティブトラックミッションのビジネスロジック
 */
class ActiveTrackMissionPresenter(private val callback: ActiveTrackMissionPresenterCallback) {

    private var activeTrackOperator: ActiveTrackOperator? = null
    private val trackModeKey: DJIKey =
        FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE)
    private var startMode = ActiveTrackMode.TRACE

    private var isAutoSensing = false

    // アクティブトラックミッションの初期化
    fun initActiveTrackMission() {
        activeTrackOperator = MissionControl.getInstance().activeTrackOperator
        activeTrackOperator?.addListener {
            onUpdate(it)
        }
    }

    // TODO 整理する
    // ミッション実行中のステータスを監視する
    private fun onUpdate(event: ActiveTrackMissionEvent) {
        val sb = StringBuffer()
        val errorInformation = (if (event.error == null) "null" else event.error!!
            .description) + "\n"
        val currentState = if (event.currentState == null) "null" else event.currentState!!
            .name
        val previousState = if (event.previousState == null) "null" else event.previousState!!
            .name
        var targetState: ActiveTrackTargetState? = ActiveTrackTargetState.UNKNOWN
        if (event.trackingState != null) {
            targetState = event.trackingState!!.state
        }
        addLineToSB(sb, "CurrentState: ", currentState)
        addLineToSB(sb, "PreviousState: ", previousState)
        addLineToSB(sb, "TargetState: ", targetState)
        addLineToSB(sb, "Error:", errorInformation)
        val value: Any? = KeyManager.getInstance().getValue(trackModeKey)
        if (value is ActiveTrackMode) {
            addLineToSB(sb, "TrackingMode:", value.toString())
        }
        val trackingState = event.trackingState
        if (trackingState != null) {
            val targetSensingInformation = trackingState.autoSensedSubjects
            if (targetSensingInformation != null) {
                for (subjectSensingState in targetSensingInformation) {
                    val trackingRect = subjectSensingState.targetRect
                    if (trackingRect != null) {
                        addLineToSB(sb, "Rect center x: ", trackingRect.centerX())
                        addLineToSB(sb, "Rect center y: ", trackingRect.centerY())
                        addLineToSB(sb, "Rect Width: ", trackingRect.width())
                        addLineToSB(sb, "Rect Height: ", trackingRect.height())
                        addLineToSB(sb, "Reason", trackingState.reason.name)
                        addLineToSB(sb, "Target Index: ", subjectSensingState.index)
                        addLineToSB(sb, "Target Type", subjectSensingState.targetType.name)
                        addLineToSB(sb, "Target State", subjectSensingState.state.name)
                        isAutoSensing = true
                    }
                }
            } else {
                val trackingRect = trackingState.targetRect
                if (trackingRect != null) {
                    addLineToSB(sb, "Rect center x: ", trackingRect.centerX())
                    addLineToSB(sb, "Rect center y: ", trackingRect.centerY())
                    addLineToSB(sb, "Rect Width: ", trackingRect.width())
                    addLineToSB(sb, "Rect Height: ", trackingRect.height())
                    addLineToSB(sb, "Reason", trackingState.reason.name)
                    addLineToSB(sb, "Target Index: ", trackingState.targetIndex)
                    addLineToSB(sb, "Target Type", trackingState.type!!.name)
                    addLineToSB(sb, "Target State", trackingState.state!!.name)
                    isAutoSensing = false
                }
                callback.clearCurrentView()
            }
        }
        callback.setResultToActiveTrackStatus(sb.toString())

        callback.updateActiveTrackRect(event)
        callback.updateButtonVisibility(event)
    }

    // StringBuilderに1行追加する
    private fun addLineToSB(stringBuilder: StringBuffer?, name: String?, value: Any?) {
        val validatedName = if(name.isNullOrEmpty().not()) "$name: " else ""
        val validatedValue = if (value == null) "" else value.toString() + ""
        stringBuilder?.append(validatedName)?.append(validatedValue)?.append("\n")
    }

    // 人を自動的に追跡するかどうか
    fun setAutoHumanTrackingEnabled(enabled: Boolean) {
        activeTrackOperator?.let {
            // 追跡有効にする設定がされた かつ ドローンに人の自動追跡機能があれば
            if (enabled && it.isAutoSensingSupported) {
                // 人の追跡機能を有効にする
                enableAutoHumanTracking(it)
            } else {
                // 人の追跡機能を無効にする
                disableAutoHumanTracking(it)
            }
        }
    }

    // 人の追跡機能を有効にする
    private fun enableAutoHumanTracking(activeTrackOperator: ActiveTrackOperator) {
        startMode = ActiveTrackMode.TRACE
        activeTrackOperator.enableAutoSensing { error ->
            val message = error?.let {
                callback.setMultiTracking(false)
                "Set AutoSensing Enabled: ${error.description}"
            } ?: run {
                "Set AutoSensing Enabled: Success"
            }
            callback.setResultToToast(message)
        }
    }

    // 人の追跡を無効にする
    private fun disableAutoHumanTracking(activeTrackOperator: ActiveTrackOperator) {
        activeTrackOperator.disableAutoSensing { error ->
            val message = error?.let {
                callback.setMultiTracking(false)
                "Disable Auto Sensing : ${it.description}"
            } ?: run {
                isAutoSensing = false
                callback.disableButtonVisibilities()
                callback.clearCurrentView()
                "Disable Auto Sensing Success!"
            }
            callback.setResultToToast(message)
        }
    }

    // 後退での追跡が有効かどうか
    fun getRetreatEnabled() {
        // API経由で後退追跡を取得する
        activeTrackOperator?.getRetreatEnabled(object :
            CommonCallbacks.CompletionCallbackWith<Boolean> {
            override fun onSuccess(result: Boolean) {
                callback.setPushBackSwitch(result)
            }

            override fun onFailure(error: DJIError?) {
                error?.let {
                    val message = "can't get retreat enable state ${it.description}"
                    callback.setResultToToast(message)
                }
            }
        })
    }

    // 後退での追跡を有効にするか
    fun setRetreatEnabled(enabled: Boolean) {
        // API経由で後退追跡を設定する
        activeTrackOperator?.setRetreatEnabled(enabled) { error ->
            val status = error?.let {
                // エラーあり
                callback.setPushBackSwitch(enabled.not())
                "Set Retreat Enabled: ${error.description}"
            } ?: run {
                "Set Retreat Enabled: Success"
            }
            // 結果を表示する
            callback.setResultToToast(status)
        }
    }

    // ジェスチャーモードが有効かどうか
    fun getGestureModeEnabled(): Boolean {
        return activeTrackOperator?.isGestureModeEnabled ?: false
    }

    // ジェスチャーモードを有効にするか
    fun setGestureModeEnabled(enabled: Boolean) {
        // API経由で設定する
        activeTrackOperator?.setGestureModeEnabled(enabled) { error ->
            val status = error?.let {
                // エラーあり
                callback.setGestureSwitch(enabled.not())
                "Set GestureMode Enabled: ${error.description}"
            } ?: run {
                "Set GestureMode Enabled: Success"
            }
            // 結果を表示する
            callback.setResultToToast(status)
        }
    }

    // マンハッタン距離を算出する
    fun calcManhattanDistance(p1X: Float, p1Y: Float, p2X: Float, p2Y: Float): Float {
        return abs(p1X - p2X) + abs(p1Y - p2Y)
    }

    // マルチトラッキングで追跡中のビューのインデックスを取得する
    fun getTrackingIndex(x: Float, y: Float, map: ConcurrentHashMap<Int, MultiTrackingView>?): Int {
        if (map.isNullOrEmpty()) {
            return ActiveTrackMissionViewController.INVALID_INDEX
        }
        var l: Float
        var t: Float
        var r: Float
        var b: Float
        for ((key, view) in map.entries) {
            l = view.x
            t = view.y
            r = view.x + view.width / 2
            b = view.y + view.height / 2
            if (x >= l && y >= t && x <= r && y <= b) {
                return key
            }
        }
        return ActiveTrackMissionViewController.INVALID_INDEX
    }

    // ビューからビデオストリームの位置(RectF)を取得する
    fun getActiveTrackRect(iv: View): RectF {
        val parent: View = iv.parent as View
        return RectF(
            (iv.left.toFloat() + iv.x) / parent.width.toFloat(),
            (iv.top.toFloat() + iv.y) / parent.height.toFloat(),
            (iv.right.toFloat() + iv.x) / parent.width.toFloat(),
            (iv.bottom.toFloat() + iv.y) / parent.height.toFloat()
        )
    }

    // 自動追跡かどうか
    fun isAutoTracking() : Boolean {
        val isAutoSensingEnabled = activeTrackOperator?.isAutoSensingEnabled ?: false
        return isAutoSensing && isAutoSensingEnabled
    }

    // 自動追跡開始
    fun startAutoSensingMission(trackingIndex: Int) {
        if (trackingIndex != ActiveTrackMissionViewController.INVALID_INDEX) {
            val mission = ActiveTrackMission(null, startMode)
            mission.targetIndex = trackingIndex
            activeTrackOperator?.startAutoSensingMission(mission) { error ->
                val message = error?.description ?: run {
                    callback.setTrackingIndex(ActiveTrackMissionViewController.INVALID_INDEX)
                    "Accept Confirm index: $trackingIndex Success!"
                }
                callback.setResultToToast(message)
            }
        }
    }

    // 確認受諾
    fun acceptConfirmation() {
        activeTrackOperator?.acceptConfirmation {
            val message = it?.description ?: run {
                callback.setTrackingIndex(ActiveTrackMissionViewController.INVALID_INDEX)
                "Accept Confirm Success!"
            }
            callback.setResultToToast(message)
        }
    }

    // 確認拒否
    fun rejectConfirmation() {
        activeTrackOperator?.rejectConfirmation {
            val message = it?.description ?: run {
                callback.setTrackingIndex(ActiveTrackMissionViewController.INVALID_INDEX)
                "Reject Confirm Success!"
            }
            callback.setResultToToast(message)
        }
    }

    // 推奨設定をONにする
    fun setRecommendedConfiguration() {
        activeTrackOperator?.setRecommendedConfiguration {
            val message = it?.let {
                "Set Recommended Config : ${it.description}"
            } ?: run {
                "Set Recommended Config : Success!"
            }
            callback.setResultToToast(message)
        }
    }

    // トラッキング開始
    fun startTracking(rectF: RectF) {
        val activeTrackMission = ActiveTrackMission(rectF, startMode)
        activeTrackOperator?.startTracking(activeTrackMission) { error ->
            val message = error?.let {
                "Start Tracking: ${it.description}"
            } ?: run {
                callback.setDrawingRect(false)
                "Start Tracking: Success"
            }
            callback.setResultToToast(message)
        }
    }

    // トラッキング停止
    fun stopTracking() {
        activeTrackOperator?.stopTracking { error ->
            val message = error?.let {
                "Stop track : ${it.description}"
            } ?: run {
                "Stop track Success!"
            }
            callback.setResultToToast(message)
        }
    }

    // 終了処理
    fun destroy() {
        activeTrackOperator?.removeAllListeners()
    }
}
