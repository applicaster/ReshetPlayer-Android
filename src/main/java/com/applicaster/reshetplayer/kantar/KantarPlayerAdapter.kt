package com.applicaster.reshetplayer.kantar

import com.applicaster.player.Player
import com.applicaster.reshetplayer.PluginParams

import de.spring.mobile.StreamAdapter
import de.spring.mobile.StreamAdapter.Meta

class KantarPlayerAdapter (val player: Player) : StreamAdapter {

    override fun getMeta(): Meta = object: Meta {

        override fun getPlayerVersion() = PluginParams.playerVersion

        override fun getPlayerName() = PluginParams.playerName

        override fun getScreenWidth() = 0

        override fun getScreenHeight() = 0

    }

    override fun getDuration(): Int = 0

    override fun getPosition(): Int = (player.videoViewWrapper.currentDate / 1000).toInt()

    override fun getWidth(): Int = 0

    override fun getHeight(): Int = 0

    override fun isCasting(): Boolean = false

}