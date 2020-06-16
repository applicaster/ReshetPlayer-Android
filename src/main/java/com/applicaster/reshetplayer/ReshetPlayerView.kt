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
import com.applicaster.atom.model.APAtomEntry.APAtomEntryPlayable
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.defaultplayer.player.ReshetPlayerViewI
import com.applicaster.reshetplayer.helpers.*
import com.applicaster.reshetplayer.kantar.KANTAR_ATTRIBUTE_STREAM_KEY
import com.applicaster.reshetplayer.kantar.kantarSensor
import com.applicaster.reshetplayer.playercontroller.*
import de.spring.mobile.Stream
import net.artimedia.artisdk.api.*
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeUnit


class ReshetPlayerView(context: Context, val playerView: ReshetPlayerViewI) : RelativeLayout(context), LifecycleObserver {

    companion object {
        const val TAG = "ReshetPlayerView"
    }

    var mPlayable : Playable? = null

    val playerContainer: ViewGroup

    /*artimedia*/
    private var api: AMSDKAPI? = null
    private var positionTimer: Subscription? = null
    private var adInProgress = false
    private var mAdInitialized = false //determined if the SDK was initialized successfully


    /*kantar stream*/
    private var stream: Stream? = null


    init {
        LayoutInflater.from(context)
                .inflate(R.layout.reshet_player_new, this, true)
        View.inflate(context, R.layout.reshet_player_new, null)

        this.playerContainer = findViewById(R.id.player_container)

        playerView.getVideoView().removeFromParent()
        this.playerContainer.addView(playerView.getVideoView())

        setMediaController()

        getActivity()!!.getLifecycle().addObserver(this)

        playerContainer.setOnClickListener {
            mCustomMediaController?.toggleMediaControllerState()
        }

    }






    fun initArtimedia(){
        val artimediaSiteName = PluginParams.artimediaSiteName
        val showAdsOnPayed = PluginParams.showAdsOnPayed

        // prepare json object

        // prepare json object
        val params = JSONObject()
        try {
            params.put("siteKey", artimediaSiteName)
            params.put("videoID", mPlayable?.getPlayableId())
            params.put("isLive", mPlayable?.isLive() ?: false|| mPlayable?.isDvr() ?: false)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val amEventListener = object: AMEventListener {
            override fun onAMSDKEvent(amEventType: AMEventType, o: Any?) {
                Log.d(TAG, amEventType.name)
                when (amEventType) {
                    AMEventType.EVT_INIT_COMPLETE ->                 //play video after init finished
                        if (o as Boolean) {
                            Log.d(TAG, "sdk initialized")
                            mAdInitialized = o
                        }
                    AMEventType.EVT_PAUSE_REQUEST -> {
                        pauseVideo()
                        playerContainer.removeView(playerView.getVideoView())
                    }
                    AMEventType.EVT_RESUME_REQUEST -> {
                        // move to EVT_LINEAR_AD_STOP
                          adInProgress = false;
                        try {
                            playerContainer.addView(playerView.getVideoView(), 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        startVideo()
                    }
                    AMEventType.EVT_AD_SHOW -> Log.d(TAG, "ad is in progress")
                    AMEventType.EVT_AD_MISSED -> adInProgress = false
                    AMEventType.EVT_LINEAR_AD_START -> adInProgress = true
                    AMEventType.EVT_LINEAR_AD_STOP -> adInProgress = false
                    AMEventType.EVT_SESSION_END -> adInProgress = false
                }
            }

        }

        api = AMSDK.getVideoAdvAPI().apply {
            registerEvent(AMEventType.EVT_INIT_COMPLETE, amEventListener)
            registerEvent(AMEventType.EVT_PAUSE_REQUEST, amEventListener)
            registerEvent(AMEventType.EVT_RESUME_REQUEST, amEventListener)
            registerEvent(AMEventType.EVT_LINEAR_AD_START, amEventListener)
            registerEvent(AMEventType.EVT_LINEAR_AD_PAUSE, amEventListener)
            registerEvent(AMEventType.EVT_LINEAR_AD_RESUME, amEventListener)
            registerEvent(AMEventType.EVT_LINEAR_AD_STOP, amEventListener)
            registerEvent(AMEventType.EVT_AD_MISSED, amEventListener)
            registerEvent(AMEventType.EVT_AD_SHOW, amEventListener)
            registerEvent(AMEventType.EVT_AD_HIDE, amEventListener)
            registerEvent(AMEventType.EVT_SESSION_END, amEventListener)
            registerEvent(AMEventType.EVT_AD_CLICK, amEventListener)
        }

        api?.initialize(AMInitParams(findViewById<View>(R.id.ad_video_frame), getArtimediaInitJsonBuilderParams()))
    }

    fun updateArtimedia() {
        api?.initialize(AMInitParams(findViewById<View>(R.id.ad_video_frame), getArtimediaInitJsonBuilderParams()))
    }

    private fun getArtimediaInitJsonBuilderParams(): AMInitJsonBuilder? {
        val initJsonBuilder = AMInitJsonBuilder(this.context.getApplicationContext())
        if (mPlayable!!.getContentVideoURL() != null) {
            try {
                initJsonBuilder.putPlacementSiteKey(PluginParams.artimediaSiteName)
                        .putPlacementCategory(mPlayable!!.getPlayableId())
                        .putPlacementIsLive(mPlayable!!.isLive())
                        .putContentId(mPlayable!!.getPlayableId())
                        .putContentVideoUrl(URLEncoder.encode(mPlayable!!.getContentVideoURL(), "UTF-8"))
                if (!mPlayable!!.isLive() && mPlayable!! is APAtomEntryPlayable) {
//                             initJsonBuilder.putContentDuration();
                    if (mPlayable!!.getContentType() != null) {
                        initJsonBuilder.putContentType(mPlayable!!.getContentType())
                    }
                    if (mPlayable!!.getContentProgramName() != null) {
                        initJsonBuilder.putContentProgramName(mPlayable!!.getContentProgramName())
                    }
                    if (mPlayable!!.getContentSeason() != null) {
                        initJsonBuilder.putContentSeason(mPlayable!!.getContentSeason())
                    }
                    if (mPlayable!!.getContentAudience() != null) {
                        initJsonBuilder.putContentTargetAudience(mPlayable!!.getContentAudience())
                    }
                    if (mPlayable!!.getContentEpisode() != null) {
                        initJsonBuilder.putContentEpisode(mPlayable!!.getContentEpisode())
                    }
                    if (mPlayable!!.getContentGenre() != null) {
                        initJsonBuilder.putContentGenre(mPlayable!!.getContentGenre())
                    }
                }

                //  .putSubscriberId("87c1363b-70a9-4c69-ad01-7af8c13ddc87")
                //  .putSubscriberPlan("family")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        return initJsonBuilder
    }


    fun setPlayable(playable: Playable) {
        mPlayable = playable
        initArtimedia()

        updateMediaController()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (api != null && adInProgress) {
            api?.resumeAd()
        }
        Log.d(TAG, "activity onResume")
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        if (api != null && adInProgress) {
            api?.pauseAd()
        }
        Log.d(TAG, "activity onPause")
        pauseVideo()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        dismissTimer()
        if (api != null) {
            api?.destroy()
        }
        Log.d(TAG, "sdk destroyed")
    }

    fun startVideo() {
        mCustomMediaController?.show()
        playerView.start()
        if (!adInProgress) {
            //super.startVideo()
            Log.d(TAG, "starting video")
            if (mAdInitialized && api != null && (positionTimer == null || positionTimer!!.isUnsubscribed())) {
                api?.updateVideoState(AMContentState.VIDEO_STATE_PLAY)
                positionTimer = Observable
                        .interval(1, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .doOnNext { sec: Long? ->
                            var pos = Math.ceil(playerView.getCurrentPosition() / 1000.toDouble()).toFloat()
                            if (mPlayable?.isDvr() ?: false || mPlayable?.isLive() ?: false) {
                                pos = Math.ceil((playerView.getCurrentDate() ?: 0) / 1000.toDouble()).toFloat()
                            }
                            if (api != null && pos > 0) {
                                api!!.updateVideoTime(pos)
                            }
                        }.subscribe()
            }
        }
        if (mPlayable!!.isLive()) {
            startKantarStream()
        }
    }

    fun pauseVideo() {
        playerView.pause()
        Log.d(TAG, "pausing video")
        dismissTimer()
        api?.updateVideoState(AMContentState.VIDEO_STATE_PAUSE)
        stopKantarStream()
    }

    fun stopVideo() {
//        super.stopVideo()
        playerView.stopPlayback()
        Log.d(TAG, "stopping video")
        if (api != null && !adInProgress) {
            api!!.updateVideoState(AMContentState.VIDEO_STATE_STOP)
        }
    }

    private fun dismissTimer() {
        if (positionTimer != null && !positionTimer!!.isUnsubscribed()) {
            positionTimer!!.unsubscribe()
            Log.d(TAG, "dismissed timer")
        }
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

        val params = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.addRule(ALIGN_PARENT_TOP)
        mCustomMediaController = ReshetPlayerMediaControllerNew(getActivity(), null)

        playerContainer.addView(mCustomMediaController as APMediaController?, params)

       updateMediaController()

        mCustomMediaController!!.initView()
    }

    fun updateMediaController(){
        mCustomMediaController!!.setDefaultVisibility()
        mCustomMediaController!!.setPlayer(playerView)
        mCustomMediaController!!.setIsLive(mPlayable?.isLive() ?: false)
        mCustomMediaController!!.setPlayableItem(mPlayable)
        mCustomMediaController!!.setFullScreenCallback(fullscreenCallback)
        mCustomMediaController!!.setVolumeCallback(setVolumeCallback)
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