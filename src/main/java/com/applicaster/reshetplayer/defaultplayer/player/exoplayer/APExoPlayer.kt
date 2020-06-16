package com.applicaster.reshetplayer.defaultplayer.player.exoplayer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.C

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource

import java.lang.ref.WeakReference

import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ui.PlayerView
import java.util.*

class APExoPlayer private constructor(val context: WeakReference<Context>) {

    private var simpleExoPlayerView: PlayerView? = null
    private var trackSelector: TrackSelectorI? = null
    private var parentContainer: WeakReference<ViewGroup>? = null
    private var contentMediaSource: MediaSource? = null
    private var displayControls: Boolean = false
    private var eventListener: Player.EventListener? = null
    private var player: SimpleExoPlayer? = null

    val isPlaying: Boolean
        get() {
            player?.let { player ->
                return (player.playbackState != Player.STATE_IDLE
                    && player.playbackState != Player.STATE_ENDED
                    && player.playWhenReady)
            }

            return false
        }

    val currentPosition: Long
        get() {
            return player?.currentPosition ?: -1
        }

    val currentDate: Long?
        get() {
            return player?.let { player ->
                val timeline = player.getCurrentTimeline()

                if (timeline.isEmpty) {
                    // Haven't got a non-empty timeline yet. Wait for onTimelineChanged() and try again
                    null
                } else {
                    val windowStartTimeMs = timeline.getWindow(player.currentWindowIndex, Timeline.Window()).windowStartTimeMs
                    if (windowStartTimeMs == C.TIME_UNSET) {
                        // HLS playlist didn't have EXT-X-PROGRAM-DATE-TIME tag
                        null
                    } else {
                        windowStartTimeMs + player.currentPosition
                    }
                }
            }
        }

    val volume: Float?
        get() {
            return player?.volume
        }

    fun positionFromDate(date: Date?): Int? {
        if (date == null) {
            return null
        }

        return player?.let { player ->
            val timeline = player.currentTimeline

            return if (timeline.isEmpty) {
                // Haven't got a non-empty timeline yet. Wait for onTimelineChanged() and try again
                null
            } else {
                val windowStartTimeMs = timeline.getWindow(player.currentWindowIndex, Timeline.Window()).windowStartTimeMs
                if (windowStartTimeMs == C.TIME_UNSET) {
                    // HLS playlist didn't have EXT-X-PROGRAM-DATE-TIME tag
                    null
                } else {
                    val seekPositionInWindowMs = date.time - windowStartTimeMs

                    seekPositionInWindowMs.toInt()
                }
            }
        }
    }


    val duration: Long
        get() {
            return player?.duration ?: 0
        }

    val isSubtitlesEnabled: Boolean
        get() = trackSelector?.isSubtitlesEnabled(player) ?: false

    val playerView: View?
        get() = simpleExoPlayerView

    /***
     * init the exo player
     */
    fun init() {
        player?.addListener(eventListener)
        context.get()?.let { context ->
            simpleExoPlayerView = PlayerView(context)
        }

        // Prepare the player with the source.
        simpleExoPlayerView?.setPlayer(player)
        simpleExoPlayerView?.useController = displayControls
        parentContainer?.get()?.addView(simpleExoPlayerView)
    }

    fun setMediaSource(source: MediaSource?) {
        contentMediaSource = source
    }

    /***
     * prepare the player for playing the video
     * @param playWhenReady should be true true if you want to play the video automatically (when video ready to play), otherwise false.
     */
    fun prepare(playWhenReady: Boolean) {
        player?.prepare(contentMediaSource)
        player?.playWhenReady = playWhenReady
    }

    /***
     * prepare the player for playing the video
     * @param playWhenReady should be true true if you want to play the video automatically (when video ready to play), otherwise false.
     * @param resetPosition should be true true if you want to play from start, false for play from last position.
     * @param resetState should be true unless the player is being prepared to play the same media as it was playing previously
     */
    fun prepare(playWhenReady: Boolean, resetPosition: Boolean, resetState: Boolean) {
        player?.playWhenReady = playWhenReady
        contentMediaSource?.let { player?.prepare(it, resetPosition, resetState) }

    }

    fun pause() {
        if (player?.playbackState == STATE_READY) {
            player?.playWhenReady = false
        }
    }

    fun play() {
        if (player?.playbackState == STATE_READY) {
            player?.playWhenReady = true
        }
    }

    fun stop() {
        player?.stop()
    }

    fun seekTo(position: Int) {
        player?.seekTo(position.toLong())
    }

    fun releasePlayer() {
        player?.release()
    }

    fun displaySubtitles() {
        trackSelector?.displaySubtitle(player)
    }

    fun removeSubtitles() {
        trackSelector?.hideSubtitle(player)
    }

    fun setDisplayControls(displayControls: Boolean) {
        this.displayControls = displayControls
        simpleExoPlayerView?.useController = displayControls
    }

    fun next() {
        player?.hasNext()?.takeIf { p -> p }.let { player?.next() }
    }

    fun prev() {
        player?.hasPrevious()?.takeIf { p -> p }.let { player?.previous() }
    }

    fun setVolume(volumeLevel: Float) {
        player?.volume = volumeLevel
    }

    /**
     * Builder class to construct for you a well formatted [APExoPlayer]
     * Created by Elad on 1/17/18.
     */
    class APExoPlayerBuilder(context: Context) {
        // Mandatory props
        private val context: WeakReference<Context>

        // Optional props
        private var trackSelector: TrackSelectorI? = null
        private var parentContainer: WeakReference<ViewGroup>? = null
        private var contentMediaSource: MediaSource? = null
        private var displayControls: Boolean = false
        private var eventListener: Player.EventListener? = null
        private var simpleExoPlayer: SimpleExoPlayer? = null

        init {
            this.context = WeakReference(context)
        }

        fun setTrackSelector(trackSelector: TrackSelectorI): APExoPlayerBuilder {
            this.trackSelector = trackSelector
            return this
        }

        fun setEventListener(eventListener: Player.EventListener): APExoPlayerBuilder {
            this.eventListener = eventListener
            return this
        }

        fun setDisplayControls(displayControls: Boolean): APExoPlayerBuilder {
            this.displayControls = displayControls
            return this
        }

        fun setExoPlayer(player: SimpleExoPlayer): APExoPlayerBuilder {
            this.simpleExoPlayer = player
            return this
        }

        fun setExoPlayerParent(parentContainer: ViewGroup): APExoPlayerBuilder {
            this.parentContainer = WeakReference(parentContainer)
            return this
        }

        fun setMediaSource(mediaSource: MediaSource): APExoPlayerBuilder {
            this.contentMediaSource = mediaSource
            return this
        }

        fun build(): APExoPlayer {
            val exoPlayer = APExoPlayer(this.context)
            exoPlayer.player = this.simpleExoPlayer
            exoPlayer.contentMediaSource = this.contentMediaSource
            exoPlayer.displayControls = this.displayControls
            exoPlayer.parentContainer = this.parentContainer
            exoPlayer.trackSelector = this.trackSelector
            exoPlayer.eventListener = this.eventListener
            return exoPlayer
        }
    }
}
