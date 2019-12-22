package com.applicaster.reshetplayer.helpers

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.parseServerDate

const val VIDEO_START_TIME_EXTENTION = "video_start_time"
const val DVR_IDENTIFIER = "dvr=true"

fun Playable.getVideoStartTime() : Long? {

    return when (this){
        is APAtomEntry.APAtomEntryPlayable ->
            (this.entry.extensions.get(VIDEO_START_TIME_EXTENTION) as? Double)?.toLong()
        is APVodItem ->
            (this.getExtension(VIDEO_START_TIME_EXTENTION) as? Double)?.toLong()
        else -> 0L
    }
}

fun Playable.getVideoId() : String? = try { (this as APAtomEntry.APAtomEntryPlayable).entry.extensions["video_id"] as String} catch (e:Exception){ "" }

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

fun Playable.isDvr() = this.contentVideoURL.contains(DVR_IDENTIFIER, ignoreCase = true)