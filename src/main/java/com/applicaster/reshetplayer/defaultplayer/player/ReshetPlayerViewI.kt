package com.applicaster.reshetplayer.defaultplayer.player

import android.view.View
import com.applicaster.player.wrappers.PlayerView
import com.applicaster.plugin_manager.playersmanager.Playable

interface ReshetPlayerViewI : PlayerView {
    fun getVideoView(): View
    fun getPlayable(): Playable
}