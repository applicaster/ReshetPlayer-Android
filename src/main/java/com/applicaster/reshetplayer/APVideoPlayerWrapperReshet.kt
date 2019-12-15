package com.applicaster.reshetplayer

import android.view.Gravity
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.applicaster.player.ApplicasterVideoPlayer
import com.applicaster.player.wrappers.ExoPlayerWrapperFactory
import com.applicaster.util.ui.APVideoViewWrapper


class APVideoViewWrapperReshet(context: Context, attrs: AttributeSet) : APVideoViewWrapper(context, attrs) {

    override fun initVideoView(type: VideoViewTypes) {
        this.type = type
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        params.gravity = Gravity.CENTER

        mVideoView = applicasterPlayerWrapperFactory.getExoPlayerWrapper(mContext)

        if (type != VideoViewTypes.MEDIAPLAYER) {
            mVideoView.setLayoutParams(params)
            this.addView(mVideoView.playerView)
        }
    }

    companion object {

        internal val applicasterPlayerWrapperFactory: ExoPlayerWrapperFactory = ApplicasterVideoPlayer()
    }
}