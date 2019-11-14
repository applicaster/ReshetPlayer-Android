package com.applicaster.reshetplayer

import com.applicaster.app.CustomApplication
import de.spring.mobile.SpringStreams

val kantarSensor: SpringStreams by lazy { SpringStreams.getInstance("C13androidapp", "reshet first", CustomApplication.getAppContext()) }

