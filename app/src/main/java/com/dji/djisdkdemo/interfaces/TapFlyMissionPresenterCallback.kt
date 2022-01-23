package com.dji.djisdkdemo.interfaces

import android.graphics.PointF

/**
 * TapFlyMissionPresenterからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface TapFlyMissionPresenterCallback {
    // トーストに表示する
    fun setResultToToast(message: String)

    // スタートボタンを表示・非表示にする
    fun setVisibilityStartButton(isVisible: Boolean)

    // ストップボタンを表示・非表示にする
    fun setVisibilityStopButton(isVisible: Boolean)

    // タップ地点の表示を非表示にする
    fun setVisibilityResultPointView(isVisible: Boolean)

    // 動画ストリーム上の位置をビューの位置に反映する
    fun showPointByTapFlyPoint(point: PointF)

    // タップフライの状態を表示する
    fun setResultToTapFlyStatus(status: String)
}
