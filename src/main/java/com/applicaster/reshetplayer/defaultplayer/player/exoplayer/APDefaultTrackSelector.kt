package com.applicaster.reshetplayer.defaultplayer.player.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * this class use the default exo player track selector
 * as a default exo player selector.
 * Created by Elad on 1/17/18.
 */

class APDefaultTrackSelector : TrackSelectorI {

    override var trackSelector: DefaultTrackSelector? = null
    private val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter()

    override val transferListener: TransferListener?
        get() = bandwidthMeter

    init {
        // Measures bandwidth during playback. Can be null if not required.
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
    }

    override fun isSubtitlesEnabled(player: ExoPlayer?): Boolean {
        var containsSubtitle = false
        trackSelector?.currentMappedTrackInfo?.let { mappedTrackInfo ->
            for (i in 0 until mappedTrackInfo.length) {
                val trackGroups = mappedTrackInfo.getTrackGroups(i)
                if (trackGroups.length != 0 && player != null) {
                    if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                        containsSubtitle = true
                    }
                }
            }
        }
        return containsSubtitle
    }

    override fun displaySubtitle(player: ExoPlayer?) {
        trackSelector?.currentMappedTrackInfo?.let { mappedTrackInfo ->
            mappedTrackInfo?.let { mappedTrackInfo ->
                for (rendererIndex in 0 until mappedTrackInfo.length) {
                    val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
                    if (trackGroups.length != 0 && player != null) {
                        if (player.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                            trackSelector?.setSelectionOverride(rendererIndex, trackGroups, trackSelector?.getSelectionOverride(rendererIndex, trackGroups))
                        }
                    }
                }
            }
        }
    }

    override fun hideSubtitle(player: ExoPlayer?) {
        trackSelector?.currentMappedTrackInfo?.let { mappedTrackInfo ->
            for (rendererIndex in 0 until mappedTrackInfo!!.length) {
                val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
                if (trackGroups.length != 0 && player != null) {
                    if (player.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                        trackSelector?.clearSelectionOverrides(rendererIndex)
                    }
                }
            }
        }
    }
}
