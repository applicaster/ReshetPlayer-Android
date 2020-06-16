package com.applicaster.reshetplayer.playercontroller

interface FullscreenCallback {
    /**
     * When triggered, the activity should hide any additional views.
     */
    fun onGoToFullscreen(currPosition: Int)

    /**
     * When triggered, the activity should show any views that were hidden when the player
     * went to fullscreen.
     */
    fun onReturnFromFullscreen(isVideoEnded: Boolean)
}