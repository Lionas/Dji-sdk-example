package com.dji.djisdkdemo.interfaces

/**
 * ActiveTrackMissionViewControllerからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface ActiveTrackMissionViewControllerCallback {
    // トーストに表示する
    fun setResultToToast(message: String)
}
