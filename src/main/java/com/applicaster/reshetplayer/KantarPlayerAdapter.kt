package com.applicaster.reshetplayer

import com.applicaster.player.Player

import de.spring.mobile.StreamAdapter
import de.spring.mobile.StreamAdapter.Meta

class KantarPlayerAdapter (val player: Player) : StreamAdapter {

    override fun getMeta(): Meta = object: Meta {

        override fun getPlayerVersion() = "player version"

        override fun getPlayerName() = "player name"

        override fun getScreenWidth() = 24

        override fun getScreenHeight() = 50

    }

    override fun getDuration(): Int = player.duration

    override fun getPosition(): Int = player.currentPosition.toInt()

    override fun getWidth(): Int = 100

    override fun getHeight(): Int = 50

    override fun isCasting(): Boolean = false

}