package com.applicaster.reshetplayer

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.applicaster.activities.base.APBaseActivity
import com.applicaster.atom.model.APAtomEntry.APAtomEntryPlayable
import com.applicaster.model.APChannel
import com.applicaster.model.APModel
import com.applicaster.player.BasePlayerConfiguration
import com.applicaster.player.Player
import com.applicaster.plugin_manager.cast.CastPlugin
import com.applicaster.plugin_manager.cast.ChromecastManager
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.defaultplayer.player.ApplicasterVideoPlayerContract
import com.applicaster.reshetplayer.defaultplayer.player.wrapper.ReshetPlayerWrapper
import java.util.*

class ReshetPlayerActivity: APBaseActivity() {

    companion object {
        const val PLAYABLE_KEY = "playable_key"
        const val VIDEO_TIME = "video_time"
    }

    lateinit var playerViewContainer: ViewGroup
    lateinit var playerView: ReshetPlayerView
    lateinit var playable: Playable

    protected var videoCurrentPosition = 0

//    override fun onPause() {
//        super.onPause()
//        savePosition()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if(videoCurrentPosition > 0 && applicasterVideoPlayerContract != null) {
//            restorePosition()
//            playerView.playerView.start()
//        }
//    }


    protected var castPlugin: CastPlugin? = null

    var applicasterVideoPlayerContract: ApplicasterVideoPlayerContract? = null

    var playerConfig: BasePlayerConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reshet_activity)

        playerViewContainer = findViewById(R.id.player_view_contianer)

        val playableIndex = 0
        //android 4.4 and below have bug, copy the arrat to playable [] will avoid crash.
        val arrayDataObject = intent.getSerializableExtra(Player.PLAYABLE_KEY) as Array<Any>
        val playableArray = Arrays.copyOf(arrayDataObject, arrayDataObject.size, Array<Playable>::class.java)

        playable = playableArray.get(playableIndex)

        getSecuredLink()

        if (isActivityRestored) {
            videoCurrentPosition = savedInstanceState!!.getInt(Player.SAVED_CURRENT_POSITION, 0)
        }

    }

    fun initiliezed() {
        if(applicasterVideoPlayerContract != null) {
            applicasterVideoPlayerContract?.removeInline(playerViewContainer)
        }
        applicasterVideoPlayerContract = StartHere()

        applicasterVideoPlayerContract?.init(playable, this)
        applicasterVideoPlayerContract?.playerWrapper = ReshetPlayerWrapper(this)
        applicasterVideoPlayerContract?.playerWrapper?.setPlayableList(mutableListOf(playable))
        applicasterVideoPlayerContract?.setVolumeController()

        playerView = applicasterVideoPlayerContract?.playerWrapper!!.reshetPlayerView

        val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        playerViewContainer.addView(playerView, 0, layoutParams)

        try {
            castPlugin = ChromecastManager.getInstance().chromecastPlugin
            if (castPlugin != null) {
                castPlugin?.init(this, lifecycle, getPlayerConfig())
            }
        } catch (e: Exception) {
            Log.e("player", "cast error: " + e.message)
        }

        if (castPlugin != null && castPlugin!!.shouldPlayWithCast()) {
            getPlayerConfig()?.let { castPlugin!!.startCasting(it) }
            playerView.playerView.stopPlayback()
            finish()

        }

        if(videoCurrentPosition > 0 && applicasterVideoPlayerContract != null) {
            restorePosition()
            playerView.playerView.start()
        }
    }


    private fun getSecuredLink() {
        // replace our local playable with the loaded one.
        if (!playable.isLive) {
            val videoId = playable.playableId
            if (videoId != null && !videoId.isEmpty()) {
                getVideoSrc(videoId, object : CallbackResponseOVidius {
                    override fun onError(e: String) {
                        Log.e("onError", e)
                        showErrorScreen()
                    }

                    override fun onSucceed(result: String) {
                        (playable as APAtomEntryPlayable).entry.content.src = result
                        playable.setContentVideoUrl(result)
                        initiliezed()
                    }
                })
            }
        } else {
            getLiveSrc(object : CallbackResponseOVidius {
                override fun onSucceed(result: String) {
                    if (playable is APChannel) {
                        (playable as APChannel).stream_url = result
                    }
                    playable.setContentVideoUrl(result)
//                    playable.setContentVideoUrl("https://reshet-live.ctedgecdn.net/13tv-desktop/r13.m3u8?DVR?")
//                    playable.setContentVideoUrl("https://reshet-vod-il.ctedgecdn.net/reshet-vod/_definst_/amlst:mediaroot/nana10/media/iiscdn/2020/06/15/mood-survivor-season-04-vip-episodes-16-full_,550,850,1400.mp4/playlist.m3u8?ctoken=522345ee51f1010788238407251a10c7baee874bcb37f3d61c484acd8b97c311&str=1592300760&exp=1592301960")
                    initiliezed()
                }

                override fun onError(e: String) {
                    Log.e("onError", e)
                    showErrorScreen()
                }
            })
        }
    }

    fun getPlayerConfig(): Map<String?, Any?>? {
        val playerConfig: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        playerConfig[CastPlugin.IS_PLAYER] = true
        playerConfig[CastPlugin.TITLE] = if (playable is APModel) (playable as APModel).name else playable.getPlayableName()
        playerConfig[CastPlugin.DESCRIPTION] = playable.getPlayableDescription()
        playerConfig[CastPlugin.CONTENT_VIDEO_URL] = playable.getContentVideoURL()
        playerConfig[CastPlugin.IS_LIVE] = playable.isLive()
        playerConfig[CastPlugin.VIDEO_DURATION] = 100//playerView.playerView.duration.toInt()
        playerConfig[Player.CURRENT_POSITION] = 4L//playerView.playerView.currentPosition.toInt()
        return playerConfig
    }

    private fun showErrorScreen() {
        val iv = ImageView(this)
        iv.setImageResource(R.drawable.no_video_found_bg)
        this.setContentView(iv)
    }

    fun savePosition() {
        videoCurrentPosition = applicasterVideoPlayerContract!!.playerWrapper.getCurrentPosition()
    }

    fun restorePosition() {
        applicasterVideoPlayerContract!!.playerWrapper.seekTo(videoCurrentPosition)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savePosition()
        outState.putInt(Player.SAVED_CURRENT_POSITION, videoCurrentPosition)
    }


}