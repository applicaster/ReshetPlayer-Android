package com.applicaster.reshetplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import com.applicaster.player.Player
import com.applicaster.plugin_manager.hook.ApplicationLoaderHookUpI
import com.applicaster.plugin_manager.hook.HookListener
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration
import com.applicaster.plugin_manager.playersmanager.PlayerContract
import com.applicaster.reshetplayer.defaultplayer.player.ApplicasterVideoPlayerContract
import com.applicaster.reshetplayer.playercontroller.FullscreenCallback


class StartHere : ApplicasterVideoPlayerContract(), ApplicationLoaderHookUpI, FullscreenCallback {

    var playableConfiguration: PlayableConfiguration? = null

    override fun executeOnStartup(context: Context?, listener: HookListener?) {
        listener?.onHookFinished()
    }

    override fun executeOnApplicationReady(context: Context?, listener: HookListener?) {
        listener?.onHookFinished()
    }

    override fun setPluginConfigurationParams(params: MutableMap<Any?, Any?>?) {
        PluginParams.initParams(params!!)
        super.setPluginConfigurationParams(params)
    }



    override fun attachInline(videoContainerView: ViewGroup)  {
        super.attachInline(videoContainerView)
        setFullScreenCallback(this)
    }


        override fun playInline(configuration: PlayableConfiguration?) {
            playableConfiguration = configuration
            super.playInline(configuration)

        }
        override fun getPlayerType(): PlayerContract.PlayerType = PlayerContract.PlayerType.Default


    override fun playInFullscreen(configuration: PlayableConfiguration?, requestCode: Int, context: Context) {
        val intent = Intent(context, ReshetPlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(ReshetPlayerActivity.PLAYABLE_KEY, arrayOf(firstPlayable))

        if (requestCode != PlayerContract.NO_REQUEST_CODE && context is Activity) {
            context.startActivityForResult(intent, requestCode)
        } else {
            context.startActivity(intent)
        }
    }

    override fun onGoToFullscreen(currPosition: Int) {

        playInFullscreen(playableConfiguration, 0, videoContainerView!!.context)
    }

    override fun onReturnFromFullscreen(isVideoEnded: Boolean) {
    }


    fun setVideoStartTime(){
        //        Long videoStartTime = getVideoStartTime(getFirstPlayable());

//        getFirstPlayable().setContentVideoUrl("https://reshet-live.ctedgecdn.net/13tv-desktop/r13.m3u8?DVR?");
        val playable = firstPlayable

        val shouldSeeKToVideoStartDate = false
//        if(getVideoStartTime(playable) != null && isInOne( Todo add when reshet ask to
//                new Date().getTime(),
//                getServerDeltaTime(),
//                getVideoStartTime(playable),
//                PluginParams.INSTANCE.getC1_cut_time().getHours(),
//                PluginParams.INSTANCE.getC1_cut_time().getMinuits(),
//                PluginParams.INSTANCE.getC1_window_length_time()))
//        {
//            playable.setContentVideoUrl(PluginParams.INSTANCE.getLiveStreamUrl());
//            shouldSeeKToVideoStartDate = true;
//        }

        //        if(getVideoStartTime(playable) != null && isInOne( Todo add when reshet ask to
//                new Date().getTime(),
//                getServerDeltaTime(),
//                getVideoStartTime(playable),
//                PluginParams.INSTANCE.getC1_cut_time().getHours(),
//                PluginParams.INSTANCE.getC1_cut_time().getMinuits(),
//                PluginParams.INSTANCE.getC1_window_length_time()))
//        {
//            playable.setContentVideoUrl(PluginParams.INSTANCE.getLiveStreamUrl());
//            shouldSeeKToVideoStartDate = true;
//        }

        //intent.putExtra(ReshetPlayer.NEED_TO_SEEK_START_TIME, shouldSeeKToVideoStartDate)

    }
}


