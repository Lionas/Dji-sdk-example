package com.dji.djisdkdemo.ui

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.view.setPadding
import dji.common.logics.warningstatuslogic.WarningStatusItem
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget

class TopBarPanelCustom(private val topBarPanel: TopBarPanelWidget) {

    /**
     * ステータスメッセージのテキスト色設定
     */
    fun setSystemStatusMessageTextColor(@ColorInt color: Int) {
        topBarPanel.systemStatusWidget?.setSystemStatusMessageTextColor(
            WarningStatusItem.WarningLevel.OFFLINE,
            color
        )
    }

    /**
     * ステータスメッセージの背景設定
     */
    fun setSystemStatusBackgroundDrawable(drawable: Drawable?) {
        topBarPanel.setPadding(0)
        topBarPanel.systemStatusWidget?.setSystemStatusBackgroundDrawable(drawable)
    }
}