package com.dji.djisdkdemo.presenter

import android.graphics.RectF
import android.view.View
import com.dji.djisdkdemo.interfaces.ActiveTrackMissionPresenterCallback
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.mission.MissionControl
import dji.sdk.mission.activetrack.ActiveTrackOperator
import dji.keysdk.KeyManager
import dji.keysdk.FlightControllerKey
import dji.keysdk.DJIKey
import dji.common.mission.activetrack.*

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
        val keyManager = KeyManager.getInstance()
        keyManager?.let {
            val value: Any? = KeyManager.getInstance().getValue(trackModeKey)
            if (value is ActiveTrackMode) {
                addLineToSB(sb, "TrackingMode:", value.toString())
            }
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
            error?.let {
                // エラーあり
                callback.setPushBackSwitch(enabled.not())
                callback.setResultToToast("Set Retreat Enabled: ${error.description}")
            }
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
            error?.let {
                // エラーあり
                callback.setGestureSwitch(enabled.not())
                callback.setResultToToast("Set GestureMode Enabled: ${error.description}")
            }
        }
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

    // RectF座標系からビュー座標系への変換
    fun translateFromRectToViewParams(view: View, rectF: RectF) : View {
        val parent = view.parent as View
        val l = ((rectF.centerX() - rectF.width() / 2) * parent.width).toInt()
        val t = ((rectF.centerY() - rectF.height() / 2) * parent.height).toInt()
        val r = ((rectF.centerX() + rectF.width() / 2) * parent.width).toInt()
        val b = ((rectF.centerY() + rectF.height() / 2) * parent.height).toInt()
        view.x = l.toFloat()
        view.y = t.toFloat()
        view.layoutParams.width = r - l
        view.layoutParams.height = b - t
        return view
    }

    // 確認受諾
    fun acceptConfirmation() {
        activeTrackOperator?.acceptConfirmation {
            it?.description?.let { errorMessage ->
                callback.setResultToToast(errorMessage)
            }
        }
    }

    // 確認拒否
    fun rejectConfirmation() {
        activeTrackOperator?.rejectConfirmation {
            it?.description?.let { errorMessage ->
                callback.setResultToToast(errorMessage)
            }
        }
    }

    // 推奨設定をONにする
    fun setRecommendedConfiguration() {
        activeTrackOperator?.setRecommendedConfiguration {
            it?.description?.let { errorMessage ->
                callback.setResultToToast("Set Recommended Config : $errorMessage")
            }
        }
    }

    // トラッキング開始
    fun startTracking(rectF: RectF) {
        val activeTrackMission = ActiveTrackMission(rectF, startMode)
        activeTrackOperator?.startTracking(activeTrackMission) {
            it?.description?.let { errorMessage ->
                callback.setResultToToast("Start Tracking: $errorMessage")
            }
        }
    }

    // トラッキング停止
    fun stopTracking() {
        activeTrackOperator?.stopTracking {
            it?.description?.let { errorMessage ->
                callback.setResultToToast("Stop track : $errorMessage")
            }
        }
    }

    // 終了処理
    fun destroy() {
        activeTrackOperator?.removeAllListeners()
    }
}
