package com.dji.djisdkdemo.interfaces

import dji.common.product.Model

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityPresenterCallback {
    fun setStatusMessage(message: String)
    fun requestPermissions(missingPermission: MutableList<String>)
    fun setProductModel(model: Model?)
    fun notifyStatusChange()

    // ログイン成功時
    fun onLoginSuccess()
}
