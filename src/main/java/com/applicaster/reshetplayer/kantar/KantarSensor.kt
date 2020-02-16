package com.applicaster.reshetplayer.kantar

import com.applicaster.app.CustomApplication
import com.applicaster.reshetplayer.PluginParams
import de.spring.mobile.SpringStreams


val kantarSensor: SpringStreams? by lazy {

    if (PluginParams.kantarSiteName.isNotBlank()) {
        SpringStreams.getInstance(PluginParams.kantarSiteName, appName, CustomApplication.getAppContext())
    } else {
        null
    }
}

val appName: String by lazy {
    val manager = CustomApplication.getAppContext().packageManager

    try {
        val info = manager.getPackageInfo(CustomApplication.getAppContext().packageName, 0)
        info.versionName

    } catch (e: Exception) {
        "reshet"
    }
}

const val KANTAR_ATTRIBUTE_STREAM_KEY = "stream"




