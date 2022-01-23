package com.dji.djisdkdemo.interfaces

import android.view.View

/**
 * TapFlyMissionViewControllerからMainActivityViewControllerの処理を呼び出すインタフェース
 */
interface TapFlyMissionViewControllerCallback {
    // トーストに表示する
    fun setResultToToast(message: String)
}
