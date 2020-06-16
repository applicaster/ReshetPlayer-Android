package com.applicaster.reshetplayer.defaultplayer.player.wrapper


import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.downloader.DownloaderUtil
import com.applicaster.model.APURLPlayable
import com.applicaster.model.APVodItem
import com.applicaster.player.wrappers.PlayerViewWrapper
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.APDefaultTrackSelector
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.APExoPlayer
import com.applicaster.reshetplayer.defaultplayer.player.exoplayer.TrackSelectorI
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.defaultplayer.player.ReshetPlayerViewI
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

open class ApplicasterPlayerWrapper(context: Context) : PlayerViewWrapper(context), ReshetPlayerViewI {

    private var playlist: List<Uri>? = null
    private lateinit var exopPlayer: APExoPlayer
    private var onPreparedListener: APVideoViewWrapper.OnPreparedListener? = null
    private var onErrorListener: APVideoViewWrapper.OnErrorListener? = null
    private var onPlaybackCompletionListener: APVideoViewWrapper.OnPlaybackCompletionListener? = null
    private var trackSelector: TrackSelectorI? = null
    private var contentMediaSource: MediaSource? = null
    private var shouldPreparePlayer = true

    override fun init(context: Context) {
        super.init(context)

        trackSelector = APDefaultTrackSelector()
        val simplePlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector?.trackSelector)

        var builder = APExoPlayer.APExoPlayerBuilder(context).setExoPlayer(simplePlayer).setDisplayControls(false)
                .setEventListener(PlayerEventListener())
        trackSelector?.let { builder.setTrackSelector(it) }
        exopPlayer = builder.build()
        exopPlayer.init()
    }

    override fun setOnPreparedListener(l: APVideoViewWrapper.OnPreparedListener) {
        onPreparedListener = l
    }

    override fun playStream(uri: Uri) {
        shouldPreparePlayer = true
        // This is the MediaSource representing the content media.
        contentMediaSource = createMediaSource(uri)
        exopPlayer.setMediaSource(contentMediaSource)
        exopPlayer.prepare(true)
    }

    fun setPlayList(playlist: List<Uri>) {
        if (playlist.size == 1) {
            setVideoURI(playlist[0])
        } else {
            this.playlist = playlist
            playPlayList(playlist)
        }
    }

    fun playPlayList(playlist: List<Uri>) {

        var concatenatedSource = ConcatenatingMediaSource()
        playlist.forEach { uri ->
            shouldPreparePlayer = true
            var mediaSource = createMediaSource(uri)
            concatenatedSource.addMediaSource(mediaSource)
        }
        contentMediaSource = concatenatedSource
        exopPlayer.setMediaSource(contentMediaSource)
        exopPlayer.prepare(true)
    }

    override fun setOnErrorListener(l: APVideoViewWrapper.OnErrorListener) {
        onErrorListener = l
    }

    override fun setOnPlaybackCompletionListener(l: APVideoViewWrapper.OnPlaybackCompletionListener) {
        onPlaybackCompletionListener = l
    }

    override fun isPlaying() = exopPlayer.isPlaying

    override fun isPaused() = !isPlaying

    override fun getCurrentPosition() = exopPlayer.currentPosition.toInt()

    override fun getCurrentDate() = exopPlayer.currentDate

    override fun getPositionFromDate(date: Date?) = exopPlayer.positionFromDate(date)

    override fun setMediaController(mediaController_: Any) {
        APLogger.error(TAG, "Exo player not support Native media controlles")
    }

    override fun toggleMediaControllerState() {
        APLogger.error(TAG, "Exo player not support Native media controlles")

    }

    override fun handleReload() = true

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {}

    override fun getPlayerView(): View? = exopPlayer.playerView

    override fun getDuration() = exopPlayer.duration.toInt()

    override fun setOnInfoListener(listener: APVideoViewWrapper.OnInfoListener) {
        onInfoListener = listener
    }

    override fun onStart() {
        if (mActivityWasStoped) {
            shouldPreparePlayer = true
            exopPlayer.prepare(false)
        }
    }

    override fun onResume() {
        super.onResume()
        exopPlayer.play()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun pause() {
        super.pause()
        exopPlayer.pause()
    }

    override fun seekTo(position: Int) {
        super.seekTo(position)
        exopPlayer.seekTo(position)
    }

    override fun start() {
        super.start()
        exopPlayer.play()
    }

    fun next() {
        exopPlayer.next()
    }

    fun prev() {
        exopPlayer.prev()
    }

    override fun stopPlayback() {
        super.stopPlayback()
        exopPlayer.stop()
    }

    override fun onRestart() {
        super.onRestart()
        //When video returns from background after power button pressed the videoView is staying visible
        //(contrary to click on home button then the videoView is not visible) and then it isn't goes to
        //the onPreapered call and the loader still visible.
        //player.setVisibility(View.GONE);
        //player.setVisibility(View.VISIBLE);
    }

    override fun onDestroy() {
        exopPlayer.releasePlayer()
    }

    private fun createMediaSource(uri: Uri): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, OSUtil.getApplicationName()), trackSelector?.transferListener)

        DownloaderUtil.getDownloaderPlugin()?.let {
            return try {
                val method = it.javaClass.getMethod("createMediaSource", Context::class.java, Uri::class.java)
                method.invoke(it, mContext, uri) as MediaSource?
                    ?: createDefaultMediaSource(uri, dataSourceFactory)
            } catch (e: Exception) {
                Log.v(TAG, e.localizedMessage)
                createDefaultMediaSource(uri, dataSourceFactory)
            }
        }
        return createDefaultMediaSource(uri, dataSourceFactory)
    }

    private fun createDefaultMediaSource(uri: Uri, dataSourceFactory: DefaultDataSourceFactory): MediaSource? {
        @C.ContentType val type = Util.inferContentType(uri)
        return when (type) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    override fun addSubtitles() {
        exopPlayer.displaySubtitles()
    }

    override fun removeSubtitles() {
        exopPlayer.removeSubtitles()
    }

    override fun isSubtitlesEnabled(): Boolean {
        return exopPlayer.isSubtitlesEnabled
    }

    fun setDisplayControls(displayControls: Boolean) {
        exopPlayer?.setDisplayControls(displayControls)
    }

    fun setVolume(volumeLevel: Float) {
        exopPlayer?.setVolume(volumeLevel)
    }

    private inner class PlayerEventListener : Player.DefaultEventListener() {

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (!shouldPreparePlayer && playbackState == STATE_ENDED) {
                onPlaybackCompletionListener?.onPlayabackCompletion(null)
            } else if (shouldPreparePlayer && !playWhenReady && playbackState == STATE_READY) {
                onPreparedListener?.onPrepared(null)
                shouldPreparePlayer = false
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {}

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

        override fun onPlayerError(error: ExoPlaybackException?) {
            onErrorListener?.onError(null, -1, -1)
        }

        override fun onPositionDiscontinuity(reason: Int) {}
    }

    companion object {
        private val TAG = ApplicasterPlayerWrapper::class.java.simpleName
    }

    override fun setPlayable(playable: Playable?) {
        super.setPlayable(playable)
        (playable as? APAtomEntry.APAtomEntryPlayable?)?.let { entry ->
            setVideoURI(Uri.parse(entry.contentVideoURL))
        }
    }

    open fun setPlayableList(playableList: MutableList<Playable>) {
        var urilist = playableList.map { playable ->
            (playable as? APAtomEntry.APAtomEntryPlayable?)?.let { entry ->
                return@map Uri.parse(entry.contentVideoURL)
            }
            (playable as? APURLPlayable)?.let { entry ->
                return@map Uri.parse(entry.contentVideoURL)
            }
            (playable as? APVodItem)?.let { entry ->
                return@map Uri.parse(entry.contentVideoURL ?: "")
            }
            Uri.EMPTY
        }.filter { p -> p != Uri.EMPTY }
        setPlayList(urilist)
    }

    override fun getVideoView(): View {
        return exopPlayer.playerView!!
    }
}
