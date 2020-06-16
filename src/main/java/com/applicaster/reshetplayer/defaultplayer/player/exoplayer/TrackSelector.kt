package com.applicaster.reshetplayer.defaultplayer.player.exoplayer

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.TransferListener

interface TrackSelectorI {

    /**
     * @return the track selector implementation.
     */
    val trackSelector: TrackSelector?

    /**
     *
     * @return the transferListener
     */
    val transferListener: TransferListener?

    /**
     * Determined if this track selector contains track (stream) with subtitle.
     *
     * @param player the exo player.
     * @return true if can display subtitle, otherwise return false.
     */
    fun isSubtitlesEnabled(player: ExoPlayer?): Boolean

    /**
     * Choose the track with the subtitle.
     *
     * @param player the exo player.
     */
    fun displaySubtitle(player: ExoPlayer?)

    /**
     * Choose the latest/default track without subtitle.
     *
     * @param player the exo player.
     */
    fun hideSubtitle(player: ExoPlayer?)
}
