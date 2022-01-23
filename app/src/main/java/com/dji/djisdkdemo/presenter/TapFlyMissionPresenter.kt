package com.dji.djisdkdemo.presenter

import android.graphics.PointF
import com.dji.djisdkdemo.interfaces.TapFlyMissionPresenterCallback
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.mission.tapfly.TapFlyMissionOperator
import dji.common.mission.tapfly.TapFlyMission
import dji.common.mission.tapfly.TapFlyMode
import dji.common.mission.tapfly.TapFlyMissionState

/**
 * タップフライミッションのビジネスロジック
 */
class TapFlyMissionPresenter(private val callback: TapFlyMissionPresenterCallback) {

    private var tapFlyMission: TapFlyMission? = null

    // タップフライミッションの初期化
    fun initTapFlyMission(isHorizontalObstacleAvoidanceEnabled: Boolean) {
        tapFlyMission = TapFlyMission()
        setHorizontalObstacleAvoidanceEnabled(isHorizontalObstacleAvoidanceEnabled)
        tapFlyMission?.tapFlyMode = TapFlyMode.FORWARD
    }

    // タップフライミッションの初期化がされているか
    fun isExistTapFlyMission(): Boolean {
        return tapFlyMission != null
    }

    // ターゲットを設定する
    fun setTarget(target: PointF?) {
        tapFlyMission?.target = target
    }

    // 水平障害物回避を設定する
    fun setHorizontalObstacleAvoidanceEnabled(isHorizontalObstacleAvoidanceEnabled: Boolean) {
        tapFlyMission?.isHorizontalObstacleAvoidanceEnabled = isHorizontalObstacleAvoidanceEnabled
    }

    // ミッションを開始する
    fun startMission() {
        tapFlyMission?.let {
            val operator = getTapFlyOperator()
            operator?.removeAllListeners()
            addListener()
            operator?.startMission(it) { error ->
                error?.let {
                    callback.setResultToToast(error.description)
                } ?: run {
                    callback.setResultToToast("Start Mission Successfully")
                    callback.setVisibilityStartButton(false)
                }
            }
        }
    }

    // ミッションを終了する
    fun stopMission() {
        val operator = getTapFlyOperator()
        operator?.stopMission { error ->
            error?.let {
                callback.setResultToToast(error.description)
            } ?: callback.setResultToToast("Stop Mission Successfully")
        }
    }

    // 自動航行の速度を設定する
    fun setAutoFlightSpeed(speed: Float) {
        getTapFlyOperator()?.setAutoFlightSpeed(speed) { error ->
            error?.let {
                callback.setResultToToast(error.description)
            } ?: callback.setResultToToast("Set Auto Flight Speed Success")
        }
    }

    // ミッション実行中のステータスを監視する
    private fun addListener() {
        val operator = getTapFlyOperator()
        operator?.addListener { aggregation ->
            aggregation?.let {
                val executionState = it.executionState
                executionState?.let { state ->
                    // 動画ストリーム中の位置を画面に反映する
                    callback.showPointByTapFlyPoint(state.imageLocation)
                }

                val sb = StringBuffer()
                addLineToSB(sb, "CurrentState: ", it.currentState.name)
                val previousState = (it.previousState?.name ?: "null")
                addLineToSB(sb, "PreviousState: ", previousState)
                val errorInformation = (it.error?.description ?: "null") + "\n"
                addLineToSB(sb, "Error:", errorInformation)

                val progressState = it.executionState
                progressState?.let { state ->
                    addLineToSB(sb, "Heading: ", state.relativeHeading)
                    addLineToSB(sb, "PointX: ", state.imageLocation.x)
                    addLineToSB(sb, "PointY: ", state.imageLocation.y)
                    addLineToSB(sb, "BypassDirection: ", state.bypassDirection.name)
                    addLineToSB(sb, "VectorX: ", state.direction.x)
                    addLineToSB(sb, "VectorY: ", state.direction.y)
                    addLineToSB(sb, "VectorZ: ", state.direction.z)
                    callback.setResultToTapFlyStatus(sb.toString())
                }

                val missionState = it.currentState
                val isRunningTapFlyMission = (
                        missionState === TapFlyMissionState.EXECUTING ||
                        missionState === TapFlyMissionState.EXECUTION_PAUSED ||
                        missionState === TapFlyMissionState.EXECUTION_RESETTING
                        )
                if (isRunningTapFlyMission) {
                    callback.setVisibilityStartButton(false)
                    callback.setVisibilityStopButton(true)
                } else {
                    callback.setVisibilityStopButton(false)
                    callback.setVisibilityResultPointView(false)
                }
            }
        }
    }

    // SDKからタップフライのミッションオペレータを取得する
    private fun getTapFlyOperator(): TapFlyMissionOperator? {
        return DJISDKManager.getInstance().missionControl?.tapFlyMissionOperator
    }

    // StringBuilderに1行追加する
    private fun addLineToSB(stringBuilder: StringBuffer?, name: String?, value: Any?) {
        val validatedName = if(name.isNullOrEmpty().not()) "$name: " else ""
        val validatedValue = if (value == null) "" else value.toString() + ""
        stringBuilder?.append(validatedName)?.append(validatedValue)?.append("\n")
    }
}