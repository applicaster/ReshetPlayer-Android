package com.applicaster.reshetplayer.helpers

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.parseServerDate

const val VIDEO_START_TIME_EXTENTION = "video_start_time"

fun Playable.getVideoStartTime() : Long {

    return when (this){
        is APAtomEntry.APAtomEntryPlayable -> try {
            parseServerDate(this.entry.getExtension(VIDEO_START_TIME_EXTENTION, String::class.java))
        } catch (e: java.lang.Exception) {
            0L
        }
        else -> 0L
    }
}
