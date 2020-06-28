package com.applicaster.reshetplayer

import android.content.Context
import android.util.Log
import android.view.View
import com.applicaster.app.CustomApplication
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.player.wrappers.PlayerView
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.helpers.*
import net.artimedia.artisdk.api.*
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

interface ArtimediaActions {
    fun resumeAd()
    fun pasueAd()
    fun relese()
    fun onVideoStarted()
    fun onVideoPause()
    fun onVideoStop()
    fun isAdInProgress(): Boolean
    fun onDestroy()
}

interface ArtimediaListner {
    fun requestPauseConent()
    fun requestResumeContant()
}

object ArtimediaManager: ArtimediaActions {

    private var mAdInitialized = false //determined if the SDK was initialized successfully
    private var adInProgress = false

    var artimediaApi: AMSDKAPI? = null
    private var artimediaListner: ArtimediaListner? = null

    private var positionTimer: Subscription? = null

    private var playerView : PlayerView? = null
    private var playable: Playable? = null
    private var adsContainer: View? = null
    private var context: Context = CustomApplication.getAppContext()



    private fun initArtimedia(){

        this.playable = playable
        this.playerView = playerView

        mAdInitialized = false
        adInProgress = false

        val amEventListener = object: AMEventListener {
            override fun onAMSDKEvent(amEventType: AMEventType, o: Any?) {
                Log.d(ReshetPlayerView.TAG, amEventType.name)
                when (amEventType) {
                    AMEventType.EVT_INIT_COMPLETE ->                 //play video after init finished
                        if (o as Boolean) {
                            Log.d(ReshetPlayerView.TAG, "sdk initialized")
                            mAdInitialized = o
                        }
                    AMEventType.EVT_PAUSE_REQUEST -> {
                        artimediaListner?.requestPauseConent()
//                        pauseVideo()
//                        playerContainer.removeView(playerView.getVideoView())
                    }
                    AMEventType.EVT_RESUME_REQUEST -> {
                        // move to EVT_LINEAR_AD_STOP
                        adInProgress = false
                        artimediaListner?.requestResumeContant()
//                        try {
//                            playerView.getVideoView().removeFromParent()
//                            playerContainer.addView(playerView.getVideoView(), 0)
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                        startVideo()
                    }
                    AMEventType.EVT_AD_SHOW -> Log.d(ReshetPlayerView.TAG, "ad is in progress")
                    AMEventType.EVT_AD_MISSED -> adInProgress = false
                    AMEventType.EVT_LINEAR_AD_START -> adInProgress = true
                    AMEventType.EVT_LINEAR_AD_STOP -> adInProgress = false
                    AMEventType.EVT_SESSION_END -> adInProgress = false
                }
            }

        }

        artimediaApi = AMSDK.getVideoAdvAPI().apply {
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

        artimediaApi?.initialize(AMInitParams(adsContainer, getArtimediaInitJsonBuilderParams(playable!!, context)))
    }

    private fun getArtimediaInitJsonBuilderParams(playable: Playable, context: Context): AMInitJsonBuilder? {
        val initJsonBuilder = AMInitJsonBuilder(context.getApplicationContext())
        if (playable.getContentVideoURL() != null) {
            try {
                initJsonBuilder.putPlacementSiteKey(PluginParams.artimediaSiteName)
                        .putPlacementCategory(playable.getPlayableId())
                        .putPlacementIsLive(playable.isLive())
                        .putContentId(playable.getPlayableId())
                        .putContentVideoUrl(URLEncoder.encode(playable.getContentVideoURL(), "UTF-8"))
                if (!playable.isLive() && playable is APAtomEntry.APAtomEntryPlayable) {
//                             initJsonBuilder.putContentDuration();
                    if (playable.getContentType() != null) {
                        initJsonBuilder.putContentType(playable.getContentType())
                    }
                    if (playable.getContentProgramName() != null) {
                        initJsonBuilder.putContentProgramName(playable.getContentProgramName())
                    }
                    if (playable.getContentSeason() != null) {
                        initJsonBuilder.putContentSeason(playable.getContentSeason())
                    }
                    if (playable.getContentAudience() != null) {
                        initJsonBuilder.putContentTargetAudience(playable.getContentAudience())
                    }
                    if (playable.getContentEpisode() != null) {
                        initJsonBuilder.putContentEpisode(playable.getContentEpisode())
                    }
                    if (playable.getContentGenre() != null) {
                        initJsonBuilder.putContentGenre(playable.getContentGenre())
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

    fun init(playable: Playable, adsContainer: View, playerView: PlayerView, listner: ArtimediaListner) {

        this.playable = playable
        this.adsContainer = adsContainer
        this.playerView = playerView
        this.artimediaListner = listner

        initArtimedia()
    }

    override fun relese(){
        dismissTimer()
        artimediaApi?.pauseAd()
        artimediaApi?.stopAdBreak()
        artimediaApi?.destroy()
        artimediaApi = null
    }



    override fun resumeAd() {
        if(adInProgress) {
            artimediaApi?.resumeAd()
        }
    }

    override fun pasueAd() {
        if(adInProgress) {
            artimediaApi?.pauseAd()
        }
    }

    override fun onVideoStarted() {
        if (!adInProgress) {
            //super.startVideo()
            Log.d(ReshetPlayerView.TAG, "starting video")
            if (mAdInitialized && artimediaApi != null && (positionTimer == null || positionTimer!!.isUnsubscribed())) {
                artimediaApi?.updateVideoState(AMContentState.VIDEO_STATE_PLAY)
                positionTimer = Observable
                        .interval(1, TimeUnit.SECONDS)
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext { sec: Long? ->
                            var pos = Math.ceil(playerView?.getCurrentPosition()!! / 1000.toDouble()).toFloat()
                            if (playable?.isDvr() ?: false || playable?.isLive() ?: false) {
                                pos = Math.ceil((playerView?.getCurrentDate() ?: 0) / 1000.toDouble()).toFloat()
                            }
                            if (artimediaApi != null && pos > 0) {
                                artimediaApi!!.updateVideoTime(pos)
                            }
                        }.subscribe()
            }
        }
    }

    override fun onVideoPause() {
        dismissTimer()
        artimediaApi?.updateVideoState(AMContentState.VIDEO_STATE_PAUSE)
    }

    override fun onVideoStop() {
        Log.d(ReshetPlayerView.TAG, "stopping video")
        if ( !adInProgress) {
            artimediaApi?.updateVideoState(AMContentState.VIDEO_STATE_STOP)
        }
    }

    override fun isAdInProgress(): Boolean {
        return adInProgress
    }

    override fun onDestroy() {
        dismissTimer()
        artimediaApi?.destroy()
        mAdInitialized = false
        adInProgress = false
    }

    private fun dismissTimer() {
        if (positionTimer != null && !positionTimer!!.isUnsubscribed()) {
            positionTimer!!.unsubscribe()
            Log.d(ReshetPlayerView.TAG, "dismissed timer")
        }
    }

}