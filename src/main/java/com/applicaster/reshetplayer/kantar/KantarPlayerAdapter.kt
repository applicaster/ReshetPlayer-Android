package com.applicaster.reshetplayer.kantar

import com.applicaster.reshetplayer.PluginParams

import de.spring.mobile.StreamAdapter
import de.spring.mobile.StreamAdapter.Meta

interface VideoData {
    fun getCurrentVideoDate(): Long
}

class KantarPlayerAdapter (val videoData: VideoData) : StreamAdapter {

    override fun getMeta(): Meta = object: Meta {

        override fun getPlayerVersion() = PluginParams.playerVersion

        override fun getPlayerName() = PluginParams.playerName

        override fun getScreenWidth() = 0

        override fun getScreenHeight() = 0

    }

    override fun getDuration(): Int = 0

    override fun getPosition(): Int = (videoData.getCurrentVideoDate() / 1000).toInt()

    override fun getWidth(): Int = 0

    override fun getHeight(): Int = 0

    override fun isCasting(): Boolean = false

}