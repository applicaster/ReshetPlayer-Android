package com.applicaster.reshetplayer.defaultplayer.player

import android.view.View
import com.applicaster.player.wrappers.PlayerView

interface ReshetPlayerViewI : PlayerView {
    fun getVideoView(): View
}