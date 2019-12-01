package com.applicaster.reshetplayer.helpers

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.parseServerDate

const val VIDEO_START_TIME_EXTENTION = "video_start_time"
const val DVR_IDENTIFIER = "DVR"

fun Playable.getVideoStartTime() : Long? {

    return when (this){
        is APAtomEntry.APAtomEntryPlayable ->
            this.entry.getExtension(VIDEO_START_TIME_EXTENTION, Long::class.java)
        is APVodItem ->
            this.getExtension(VIDEO_START_TIME_EXTENTION) as Long
        else -> 0L
    }
}

fun Playable.setVideoStartTime(startTime: Long) {
    when (this){
        is APAtomEntry.APAtomEntryPlayable ->
            this.entry.setExtension(VIDEO_START_TIME_EXTENTION, startTime)
        is APVodItem -> {
            this.setExtension(VIDEO_START_TIME_EXTENTION, startTime)
        }
        else -> 0L
    }
}

fun Playable.isDvr() = this.contentVideoURL.contains(DVR_IDENTIFIER)