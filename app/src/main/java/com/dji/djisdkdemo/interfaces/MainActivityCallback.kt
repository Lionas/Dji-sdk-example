package com.dji.djisdkdemo.interfaces

/**
 * MainActivityPresenterからMainActivityの処理を呼び出すインタフェース
 */
interface MainActivityCallback {
    // トーストの表示
    fun showToast(message: String)

    // ステータス表示エリアの設定
    fun setStatusMessage(message: String)

    // パーミッションリクエスト
    fun requestPermissions(missingPermission: MutableList<String>)

    // 接続中の製品表示エリアの設定
    fun setProduct(name: String)

    // 接続状態の変化通知
    fun notifyStatusChange()

    // ログイン成功
    fun onLoginSuccess(message: String)

    // ログイン失敗
    fun onLoginFailure(message: String)
}
