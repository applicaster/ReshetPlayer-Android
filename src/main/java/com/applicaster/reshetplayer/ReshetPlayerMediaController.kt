package com.applicaster.reshetplayer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.applicaster.player.controller.APMediaController
import com.applicaster.player.controller.MidrollView
import com.applicaster.util.OSUtil
import com.applicaster.util.StringUtil
import java.util.*
import java.util.concurrent.TimeUnit

open class ReshetPlayerMediaController(context: Context?, attrs: AttributeSet?) : APMediaController(context, attrs) {

    override fun initSeekBar() {
        timerContainer = findViewById(OSUtil.getResourceId("timer_container"))
        seekbarContainer = findViewById(OSUtil.getResourceId("seekbarContainer"))
        elapsedTime = findViewById<View>(OSUtil.getResourceId("time_elapsed")) as TextView
        //timeSeparator  = (TextView) findViewById(OSUtil.getResourceId("time_seperator"));
        totalTime = findViewById<View>(OSUtil.getResourceId("total_time")) as TextView

        midrollImageView = findViewById<View>(OSUtil.getResourceId("midroll_image")) as MidrollView
        seekbar = findViewById<View>(OSUtil.getResourceId("seekbar")) as SeekBar
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.max > TimeUnit.SECONDS.toMillis(30) && (seekBar.max - seekBar.progress) <= TimeUnit.SECONDS.toMillis(30)) {
                    playerSeekTo(seekBar.max - TimeUnit.SECONDS.toMillis(30).toInt())
                }
                else {
                    playerSeekTo(seekBar.progress)
                }
                seekingActionInProgress = false
                startCurrentPositionTimer()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekingActionInProgress = true
                player.seekStart(seekBar.progress)
                stopCurrentPositionTimer()
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    elapsedTime.text = progress.parseDuration()
                }
            }
        })
    }


    override fun startCurrentPositionTimer() {
        currentPositionTimer = Timer()
        currentPositionTimer.schedule(object : TimerTask() {

            override fun run() {
                mContext.runOnUiThread {
                    val currentPosition = currentPosition
                    val currentTime = currentPosition.parseDuration()
                    elapsedTime.text = currentTime
                    if (recordBtn != null && captureDuration != null && captureDuration.visibility == View.VISIBLE) {
                        val startPosition : Int? = recordBtn.tag as? Int
                        startPosition?.apply { captureDuration.text = StringUtil.parseDuration("" + (currentPosition - startPosition)) }
                    }
                    if (!seekingActionInProgress) {
                        seekbar.progress = currentPosition
                    }
                }

            }

        }, 200, 200)
    }

    private fun playerSeekTo(position: Int) {
        player.seekTo(position)
    }

    private fun Int.parseDuration() :String {
        return if (this >= 0) {
            val durationMillis = this.toLong()
            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % TimeUnit.HOURS.toMinutes(1)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % TimeUnit.MINUTES.toSeconds(1)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else "00:00:00"
    }

}