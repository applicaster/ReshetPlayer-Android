package com.applicaster.reshetplayer.helpers

import java.util.*

fun isInOne(currentPhoneTime: Long, serverDelatTime: Long, vodStartTime: Long, c1CatTimeHour: Int, c1CatTimeMinuit: Int, windowLength: Long):Boolean {

    val now = currentPhoneTime + serverDelatTime

    val isLessThenWindowLength = (now - vodStartTime) < windowLength

    return if(isLessThenWindowLength) {
        // vod startTime
        val date = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        date.time = Date(vodStartTime)
        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, c1CatTimeHour)
        date.set(Calendar.MINUTE, c1CatTimeMinuit)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if(vodStartTime < date.timeInMillis){
            date.add(Calendar.DAY_OF_MONTH, 1)
        } else {
            date.add(Calendar.DAY_OF_MONTH, 2)
        }

        val isInCOne = now < date.timeInMillis

        isInCOne
    } else {
        false
    }
}