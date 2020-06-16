package com.applicaster.reshetplayer.defaultplayer.player

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.applicaster.reshetplayer.defaultplayer.player.wrapper.ReshetPlayerWrapper
import com.applicaster.reshetplayer.playercontroller.FullscreenCallback
import com.applicaster.reshetplayer.playercontroller.SetVolumeCallback
import com.applicaster.util.OSUtil


open class ApplicasterVideoPlayerContract: PlayerContract {


    var currentInline: ApplicasterVideoPlayerContract? = null
    var videoContainerView: ViewGroup? = null
    lateinit var playerWrapper: ReshetPlayerWrapper

    private var playableList = mutableListOf<Playable>()
    var config: MutableMap<Any?, Any?>? = null
    var isOnHold = false
    var state: PlayerContract.State? = null
    var fullscreenDialog: Dialog? = null
    var initOrientation = if (OSUtil.isTablet()) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun init(appContext: Context) {
        playerWrapper = ReshetPlayerWrapper(appContext)
    }

    override fun init(playable: Playable, context: Context) {
        if (currentInline != null) {
            currentInline?.stopInline()
            currentInline?.removeInline(currentInline?.videoContainerView!!)
        }

        playerWrapper = ReshetPlayerWrapper(context)
        this.playableList = mutableListOf<Playable>(playable)
        playerWrapper.setPlayableList(this.playableList)
        setVolumeController()

//        this.playableList = mutableListOf<Playable>(playable)
    }

    override fun init(playableList: MutableList<Playable>, context: Context) {
        if (currentInline != null) {
            currentInline?.stopInline()
            currentInline?.removeInline(currentInline?.videoContainerView!!)
        }

        playerWrapper = ReshetPlayerWrapper(context)
        this.playableList = playableList
        playerWrapper.setPlayableList(this.playableList)
        setVolumeController()
    }

    fun setVolumeController(){
        playerWrapper.reshetPlayerView.setVolumeCallback(object : SetVolumeCallback{
            override fun setVolume(volume: Float) {
                playerWrapper.setVolume(volume)
            }

        })
    }

    override fun getPlayerType() = PlayerContract.PlayerType.Default

    override fun isPlayerPlaying() = playerWrapper.isPlaying()

    override fun getFirstPlayable() = playableList.first()

    override fun getPluginConfigurationParams() = config

    fun setFullScreenCallback(fullscreenCallback: FullscreenCallback) {
        playerWrapper.reshetPlayerView.setFullScreenCallBack(fullscreenCallback)
    }

    fun setVolume(volume: Float) {
        playerWrapper.setVolume(volume)
    }

    override fun getPlayableList(): MutableList<Playable> = playableList

    override fun play(configuration: PlayableConfiguration?) {
        playerWrapper.start()
    }

    override fun pause() {
        playerWrapper.pause()
    }

    override fun stop() {
        playerWrapper.pause()
        playerWrapper.seekTo(0)
    }

    override fun getPlaybackPosition() = playerWrapper.currentPosition.toLong()

    override fun seekTo(position: Long) {
        playerWrapper.seekTo(position.toInt())
    }

    override fun seekBy(delta: Long) {
        val desirePosition = playerWrapper.currentPosition + delta.toInt()
        if (desirePosition >= 0 && desirePosition <= playerWrapper.duration) {
            playerWrapper.seekTo(desirePosition)
        }
    }

    override fun moveNext() {
        playerWrapper.next()
    }

    override fun movePrev() {
        playerWrapper.prev()
    }

    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        TODO("Not yet implemented")
    }
/*
    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        if (fullscreenDialog?.isShowing != true) {
//            (playerView.playerView?.parent as? ViewGroup?)?.let { parentView ->
                initOrientation = (context as Activity).requestedOrientation
                val continuePlayingOnBack = (configuration?.customConfiguration?.get("CONTINUE_PLAYING_ON_BACK") as? Boolean?)
                    ?: false
                val requestOrientation = (configuration?.customConfiguration?.get("REQUEST_ORIENTATION") as? Int?)
                    ?: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                context.requestedOrientation = requestOrientation
                fullscreenDialog = object : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                    override fun onBackPressed() {
                        if (!continuePlayingOnBack) {
                            playerWrapper.pause()
                        }
                        context.requestedOrientation = initOrientation
                        (playerWrapper.playerView?.parent as? ViewGroup?)?.removeView(playerWrapper.playerView)
//                        parentView.addView(playerView.playerView)
                        fullscreenDialog?.dismiss()

                    }
                }
//                parentView.removeView(playerView.playerView)
//            }
            fullscreenDialog?.addContentView(playerWrapper.playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            fullscreenDialog?.show()
        }
    }

 */

    override fun attachInline(videoContainerView: ViewGroup) {
        if (fullscreenDialog?.isShowing == true) {
            (playerWrapper.playerView?.parent as? ViewGroup?)?.let { p ->
                p.removeView(playerWrapper.playerView)
                videoContainerView.addView(playerWrapper.playerView)
                (videoContainerView.context as? Activity?)?.requestedOrientation = initOrientation
            }
            fullscreenDialog?.dismiss()
        } else {
            (playerWrapper.playerView?.parent as ViewGroup?)?.let { it.removeView(playerWrapper.playerView) }
            videoContainerView.addView(playerWrapper.playerView)
        }

        this.videoContainerView = videoContainerView
        currentInline = this
    }

    override fun removeInline(videoContainerView: ViewGroup) {
        playerWrapper.stopPlayback()
        videoContainerView.removeView(playerWrapper.playerView)
    }

    override fun isPlayerOnHold() = isOnHold

    override fun isAdPlaying() = false

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        this.config = params
    }

    override fun playInline(configuration: PlayableConfiguration?) {
//        (configuration?.customConfiguration?.get("DISPLAY_NATIVE_CONTROLS") as? Boolean?)?.let {
//            playerWrapper.setDisplayControls(it)
//        }
//        playerWrapper.setDisplayControls(false)
//        playerWrapper.start()
    }

    override fun stopInline() {
        playerWrapper.stopPlayback()
    }

    override fun pauseInline() {
        playerWrapper.pause()
    }

    override fun resumeInline() {
        playerWrapper.start()
    }

    override fun setPlayerOnHold(isOnHold: Boolean) {
        this.isOnHold = isOnHold
    }

    override fun setPlayerState(playerState: PlayerContract.State?) {
        this.state = playerState
    }

    override fun getPlayerState() = this.state ?: PlayerContract.State.LoadingPlayable

    override fun setIsLazyLoadedPlayer(isLazyLoadedPlayer: Boolean) {}

    override fun isLazyLoadedPlayer() = false

}