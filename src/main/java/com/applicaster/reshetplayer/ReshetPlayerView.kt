package com.applicaster.reshetplayer


import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.defaultplayer.player.ReshetPlayerViewI
import com.applicaster.reshetplayer.kantar.KANTAR_ATTRIBUTE_STREAM_KEY
import com.applicaster.reshetplayer.kantar.kantarSensor
import com.applicaster.reshetplayer.playercontroller.*
import de.spring.mobile.Stream
import java.util.*
import com.applicaster.reshetplayer.playercontroller.ContollerType


class ReshetPlayerView(context: Context, val playerView: ReshetPlayerViewI) : RelativeLayout(context), LifecycleObserver {

    companion object {
        const val TAG = "ReshetPlayerView"
    }

    var mPlayable : Playable? = null

    val playerContainer: ViewGroup

    private var controllerType: ContollerType = ContollerType.basic

    /*kantar stream*/
    private var stream: Stream? = null


    init {
        LayoutInflater.from(context)
                .inflate(R.layout.reshet_player_new, this, true)
        View.inflate(context, R.layout.reshet_player_new, null)

        this.playerContainer = findViewById(R.id.player_container)

        this.playerContainer.addView(playerView.getVideoView())

        mPlayable = playerView.getPlayable()

        setMediaController()

        getActivity()!!.lifecycle.addObserver(this)

        playerContainer.setOnClickListener {
            mCustomMediaController?.toggleMediaControllerState()
        }

        startVideo()

    }

    fun setPlayable(playable: Playable) {
        mPlayable = playable
        ArtimediaManager.init(playable, findViewById(R.id.ad_video_frame), playerView, object : ArtimediaListner {
            override fun requestPauseConent() = pauseVideo()
            override fun requestResumeContant() = startVideo()

        })

        updateMediaController()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.d(TAG, "activity onResume")
        ArtimediaManager.resumeAd()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.d(TAG, "activity onPause")
        ArtimediaManager.pasueAd()
        pauseVideo()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.d(TAG, "activity onDestroy")
        playerView.stopPlayback()
       ArtimediaManager.relese()
    }

    fun startVideo() {
        mCustomMediaController?.show()
        playerView.start()

        ArtimediaManager.onVideoStarted()

        if (mPlayable!!.isLive()) {
            startKantarStream()
        }
    }

    fun pauseVideo() {
        playerView.pause()
        Log.d(TAG, "pausing video")
        stopKantarStream()
    }

    fun stopVideo() {
        playerView.stopPlayback()

    }


    private fun startKantarStream() {
        stopKantarStream()
        val atts: MutableMap<String, Any> = HashMap()
        atts[KANTAR_ATTRIBUTE_STREAM_KEY] = PluginParams.kantarAttributeStreamValue // mandatory
        if (kantarSensor != null) {
           // stream = kantarSensor!!.track(KantarPlayerAdapter(this), atts)
        }
    }

    private fun stopKantarStream() {
        if (stream != null) {
            stream!!.stop()
            stream = null
        }
    }

    private fun getActivity(): AppCompatActivity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    var mCustomMediaController: APMediaControllerI? = null

    protected fun setMediaController() {

        mCustomMediaController = findViewById<ReshetPlayerMediaControllerNew>(R.id.reshet_media_controller_new)

       updateMediaController()

        mCustomMediaController!!.initView()


    }

    fun setControllerType(controllerType: ContollerType) {
        this.controllerType = controllerType
        mCustomMediaController?.setControllerType(controllerType)
    }

    fun updateMediaController(){
        mCustomMediaController!!.setDefaultVisibility()
        mCustomMediaController!!.setPlayer(playerView)
        mCustomMediaController!!.setIsLive(mPlayable?.isLive() ?: false)
        mCustomMediaController!!.setPlayableItem(mPlayable)
        mCustomMediaController!!.setFullScreenCallback(fullscreenCallback)
        mCustomMediaController!!.setVolumeCallback(setVolumeCallback)
        mCustomMediaController!!.setControllerType(controllerType)
    }

    private var fullscreenCallback: FullscreenCallback? = null

    fun setFullScreenCallBack(fullscreenCallback: FullscreenCallback){
        this.fullscreenCallback = fullscreenCallback
        updateMediaController()
    }

    private var setVolumeCallback: SetVolumeCallback? = null

    fun setVolumeCallback(volumeCallback: SetVolumeCallback) {
        this.setVolumeCallback = volumeCallback
        updateMediaController()
    }

}

fun View.removeFromParent(){
    (this.parent as? ViewGroup)?.removeView(this)
}