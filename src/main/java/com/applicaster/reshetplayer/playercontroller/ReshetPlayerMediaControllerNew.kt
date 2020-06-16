package com.applicaster.reshetplayer.playercontroller

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.applicaster.player.controller.MidrollView
import com.applicaster.reshetplayer.R
import com.applicaster.reshetplayer.helpers.isDvr
import com.applicaster.util.OSUtil
import com.applicaster.util.StringUtil
import java.util.*
import java.util.concurrent.TimeUnit

open class ReshetPlayerMediaControllerNew(context: Context?, attrs: AttributeSet?) : APMediaController(context, attrs) {

    lateinit var toggleVolumeButton: View
    var setVolumeLisener: SetVolumeCallback? = null
    var isVolumeOn = true;

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
                player.onSeekStart(seekBar.progress)
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

    override fun setVolumeCallback(setVolumeCallback: SetVolumeCallback?) {
      this.setVolumeLisener = setVolumeCallback
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

    override fun show() {
        if(false) {
        //if (isLive && playable.isDvr() == false) {
            if (timerContainer != null) {
                timerContainer.visibility = View.GONE
                seekbar.visibility = View.GONE
            }
            if (seekbarContainer != null) {
                seekbarContainer.visibility = View.GONE
                seekbar.visibility = View.GONE
            }
        } else {
            val currentPosition = this.currentPosition
            val duration = player.duration
            if (!isSocialbarEnabled) {
                seekbarContainer.visibility = View.VISIBLE
            }
            seekbar.visibility = View.VISIBLE
            seekbar.max = duration
            seekbar.progress = currentPosition
            val currentTime = StringUtil.parseDuration("" + currentPosition)
            elapsedTime.text = currentTime
            val totalTimeStr = StringUtil.parseDuration("" + duration)
            totalTime.text = totalTimeStr
            (seekbar.layoutParams as LayoutParams).topMargin = -1 * seekbar.thumbOffset / 2
            startCurrentPositionTimer()
        }
        displayTopBarWithAnimation()
    }

    override fun initView() {
        super.initView()

        initVolumeView()
    }

    fun initVolumeView(){
        toggleVolumeButton = findViewById(R.id.volume)
        toggleVolumeButton.setOnClickListener {
            if(isVolumeOn) {
                isVolumeOn = false
                setVolumeLisener?.setVolume(1f)
            } else {
                isVolumeOn = true
                setVolumeLisener?.setVolume(0f)
            }
        }
    }

    override fun inflateLayout() {
        val apMediaController = findViewById<View>(OSUtil.getResourceId("reshet_media_controller")) as? ViewGroup
        if (apMediaController == null) {
            LayoutInflater.from(mContext).inflate(OSUtil.getLayoutResourceIdentifier("reshet_media_controller"), this, true)
        }
    }

}