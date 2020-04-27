package com.applicaster.reshetplayer.helpers

import com.applicaster.session.SessionStorage

const val RESHET_SERVER_DELTA_TIME = "reshet_server_delta_time"

fun getServerDeltaTime(): Long{
    val dateString = SessionStorage.get(RESHET_SERVER_DELTA_TIME)
    return if (dateString==null) 0
    else dateString.toLongOrNull() ?: 0
}

fun setSeverDelatTime(time: Long){
    SessionStorage.set(RESHET_SERVER_DELTA_TIME, time.toString())
}