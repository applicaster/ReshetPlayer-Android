package com.applicaster.reshetplayer.defaultplayer.player.wrapper


import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.downloader.DownloaderUtil
import com.applicaster.model.APURLPlayable
import com.applicaster.player.wrappers.PlayerViewWrapper
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.APDefaultTrackSelector
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.APExoPlayer
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.TrackSelectorI
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.ReshetPlayerView
import com.applicaster.util.APLogger
import com.applicaster.util.OSUtil
import com.applicaster.util.ui.APVideoViewWrapper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.util.*

class ReshetPlayerWrapper(context: Context) : ApplicasterPlayerWrapper(context) {

    lateinit var reshetPlayerView: ReshetPlayerView

    override fun init(context: Context) {
        super.init(context)
        reshetPlayerView = ReshetPlayerView(context, this)
    }

    override fun getPlayerView(): View? = reshetPlayerView


    override fun setPlayableList(playableList: MutableList<Playable>) {
        super.setPlayableList(playableList)
        reshetPlayerView.setPlayable(playableList.first())
    }

}
