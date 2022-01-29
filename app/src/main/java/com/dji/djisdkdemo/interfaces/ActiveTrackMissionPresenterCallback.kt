package com.dji.djisdkdemo.interfaces

import dji.common.mission.activetrack.ActiveTrackMissionEvent

/**
 * ActiveMissionPresenterからActiveTrackMissionViewControllerの処理を呼び出すインタフェース
 */
interface ActiveTrackMissionPresenterCallback {
    // トーストに表示する
    fun setResultToToast(message: String)

    // マルチトラッキングを設定する
    fun setMultiTracking(enabled: Boolean)

    // アクティブトラック関連ボタンの非表示
    fun disableButtonVisibilities()

    // 後退追跡有効無効スイッチを設定する
    fun setPushBackSwitch(enabled: Boolean)

    // ジェスチャーモードスイッチを設定する
    fun setGestureSwitch(enabled: Boolean)

    // アクティブトラックの状態を表示する
    fun setResultToActiveTrackStatus(status: String)

    // アクティブトラックの描画を更新する
    fun updateActiveTrackRect(event: ActiveTrackMissionEvent)

    // ボタンの表示非表示を更新する
    fun updateButtonVisibility(event: ActiveTrackMissionEvent)

    // 現在の表示を消去する
    fun clearCurrentView()

    // 選択範囲の描画中かどうか
    fun setDrawingRect(isDrawing: Boolean)

    // 追跡用インデックスの設定
    fun setTrackingIndex(index: Int)
}