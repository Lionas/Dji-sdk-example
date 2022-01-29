package com.dji.djisdkdemo.ui

import android.content.Context
import dji.common.mission.activetrack.ActiveTrackTargetState
import dji.common.mission.activetrack.SubjectSensingState
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.dji.djisdkdemo.R

/**
 * マルチトラックビュー
 */
class MultiTrackingView(context: Context?) : RelativeLayout(context) {
    private var valueIndex: TextView
    private var rectF: ImageView

    init {
        val view: View = LayoutInflater.from(context).inflate(R.layout.layout_multi_tracking, null)
        this.addView(view)
        valueIndex = findViewById<View>(R.id.index_textview) as TextView
        rectF = findViewById<View>(R.id.tracking_rectf_iv) as ImageView
    }

    fun updateView(information: SubjectSensingState) {
        when (information.state) {
            ActiveTrackTargetState.CANNOT_CONFIRM,
            ActiveTrackTargetState.UNKNOWN ->
                rectF.setImageResource(R.drawable.visual_track_cannotconfirm)
            ActiveTrackTargetState.WAITING_FOR_CONFIRMATION ->
                rectF.setImageResource(R.drawable.visual_track_needconfirm)
            ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE ->
                rectF.setImageResource(R.drawable.visual_track_lowconfidence)
            ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE ->
                rectF.setImageResource(R.drawable.visual_track_highconfidence)
            else -> Unit
        }
        valueIndex.text = information.index.toString()
    }

}