package com.dji.djisdkdemo.ui

import android.annotation.SuppressLint
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.*
import com.dji.djisdkdemo.R
import java.lang.ref.WeakReference
import android.view.MotionEvent
import android.view.View
import com.dji.djisdkdemo.presenter.TapFlyMissionPresenter
import android.graphics.PointF
import com.dji.djisdkdemo.interfaces.TapFlyMissionPresenterCallback
import com.dji.djisdkdemo.interfaces.TapFlyMissionViewControllerCallback
import dji.sdk.codec.DJICodecManager

import dji.sdk.camera.VideoFeeder
import dji.sdk.camera.VideoFeeder.VideoDataListener


class TapFlyMissionViewController(
    appCompatActivity: AppCompatActivity,
    private val callback: TapFlyMissionViewControllerCallback
) : LifecycleEventObserver {

    companion object {
        const val TAG = "TapFlyViewController"
    }
    private val weakActivityReference = WeakReference(appCompatActivity)
    private var tapFlyMissionPresenter: TapFlyMissionPresenter? = null

    private lateinit var pushDrawerIb: ImageButton
    private lateinit var pushDrawerSd: SlidingDrawer
    private lateinit var startBtn: Button
    private lateinit var stopBtn: ImageButton
    private lateinit var pushTv: TextView
    private lateinit var bgLayout: RelativeLayout
    private lateinit var rstPointIv: ImageView
    private lateinit var assisTv: TextView
    private lateinit var assisSw: SwitchCompat
    private lateinit var speedTv: TextView
    private lateinit var speedSb: AppCompatSeekBar

    @SuppressLint("ClickableViewAccessibility")
    // UIの初期化
    fun initUI() {
        weakActivityReference.get()?.let {
            pushDrawerIb = it.findViewById(R.id.pointing_drawer_control_ib) as ImageButton
            pushDrawerSd = it.findViewById(R.id.pointing_drawer_sd) as SlidingDrawer
            startBtn = it.findViewById(R.id.pointing_start_btn) as Button
            stopBtn = it.findViewById(R.id.pointing_stop_btn) as ImageButton
            pushTv = it.findViewById(R.id.pointing_push_tv) as TextView
            bgLayout = it.findViewById(R.id.pointing_bg_layout) as RelativeLayout
            rstPointIv = it.findViewById(R.id.pointing_rst_point_iv) as ImageView
            assisTv = it.findViewById(R.id.pointing_assistant_tv) as TextView
            assisSw = it.findViewById(R.id.pointing_assistant_sw) as SwitchCompat
            speedTv = it.findViewById(R.id.pointing_speed_tv) as TextView
            speedSb = it.findViewById(R.id.pointing_speed_sb) as AppCompatSeekBar

            pushDrawerIb.setOnClickListener {
                if (pushDrawerSd.isOpened) {
                    pushDrawerSd.animateClose();
                } else {
                    pushDrawerSd.animateOpen();
                }
            }
            assisSw.setOnCheckedChangeListener { _, isChecked ->
                tapFlyMissionPresenter?.setHorizontalObstacleAvoidanceEnabled(isChecked)
            }
            startBtn.setOnClickListener {
                tapFlyMissionPresenter?.startMission()
            }
            stopBtn.setOnClickListener {
                tapFlyMissionPresenter?.stopMission()
            }
            bgLayout.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        tapFlyMissionPresenter?.let {
                            if (it.isExistTapFlyMission()) {
                                startBtn.visibility = View.VISIBLE
                                startBtn.x = event.x - startBtn.width / 2
                                startBtn.y = event.y - startBtn.height / 2
                                startBtn.requestLayout()
                                tapFlyMissionPresenter?.setTarget(getTapFlyPoint(startBtn))
                                true
                            } else {
                                callback.setResultToToast("TapFlyMission is null")
                                true
                            }
                        } ?: run {
                            callback.setResultToToast("TapFlyMission is null")
                            true
                        }
                    }
                    else -> {
                        true
                    }
                }
            }
            speedSb.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    speedTv.text = (progress + 1).toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val speed = (speedSb.progress + 1).toFloat()
                    tapFlyMissionPresenter?.setAutoFlightSpeed(speed)
                }
            })
        }
    }

    // ビューのタップ位置から動画ストリーム上のタップ位置に変換する
    private fun getTapFlyPoint(view: View?): PointF? {
        if (view == null) return null
        val parent = view.parent as View

        // タップした位置が左上原点となるような中心点を算出
        // 親ビューからタップしたビューまでの距離＋タップしたビューからの距離＋タップしたビューの半分先が中心点
        var centerX = view.left + view.x + view.width.toFloat() / 2
        var centerY = view.top + view.y + view.height.toFloat() / 2
        centerX = if (centerX < 0) 0f else centerX
        centerX = if (centerX > parent.width) parent.width.toFloat() else centerX
        centerY = if (centerY < 0) 0f else centerY
        centerY = if (centerY > parent.height) parent.height.toFloat() else centerY
        return PointF(centerX / parent.width, centerY / parent.height)
    }

    // 動画ストリーム上の位置をビューの位置に反映する
    fun showPointByTapFlyPoint(point: PointF, imageView: ImageView?) {
        imageView?.let { v ->
            val parent = v.parent as View
            weakActivityReference.get().let { appCompatActivity ->
                appCompatActivity?.runOnUiThread {
                    v.x = point.x * parent.width - v.width / 2
                    v.y = point.y * parent.height - v.height / 2
                    v.visibility = View.VISIBLE
                    v.requestLayout()
                }
            }
        }
    }

    // ライフサイクルに連動する処理
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // TapFlyMissionPresenterの生成
                tapFlyMissionPresenter = TapFlyMissionPresenter(
                    object: TapFlyMissionPresenterCallback{
                        override fun setResultToToast(message: String) {
                            callback.setResultToToast(message)
                        }

                        override fun setVisibilityStartButton(isVisible: Boolean) {
                            setVisibility(startBtn, isVisible)
                        }

                        override fun setVisibilityStopButton(isVisible: Boolean) {
                            setVisibility(stopBtn, isVisible)
                        }

                        override fun setVisibilityResultPointView(isVisible: Boolean) {
                            setVisibility(rstPointIv, isVisible)
                        }

                        override fun showPointByTapFlyPoint(point: PointF) {
                            weakActivityReference.get().let { appCompatActivity ->
                                appCompatActivity?.runOnUiThread {
                                    showPointByTapFlyPoint(point, rstPointIv)
                                }
                            }
                        }

                        override fun setResultToTapFlyStatus(status: String) {
                            weakActivityReference.get().let { appCompatActivity ->
                                appCompatActivity?.runOnUiThread {
                                    pushTv.text = status
                                }
                            }
                        }
                    }
                )
            }
            Lifecycle.Event.ON_RESUME -> {
                // TapFlyMissionの初期化
                tapFlyMissionPresenter?.initTapFlyMission(assisSw.isChecked)
            }
            Lifecycle.Event.ON_PAUSE -> {
            }
            Lifecycle.Event.ON_START -> Unit
            Lifecycle.Event.ON_STOP -> Unit
            Lifecycle.Event.ON_DESTROY -> Unit
            Lifecycle.Event.ON_ANY -> Unit
        }
    }

    private fun setVisibility(view: View, isVisible: Boolean) {
        weakActivityReference.get().let { appCompatActivity ->
            appCompatActivity?.runOnUiThread {
                view.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            }
        }
    }
}