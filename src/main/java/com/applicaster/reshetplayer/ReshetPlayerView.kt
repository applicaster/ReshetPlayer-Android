package com.applicaster.reshetplayer


import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewpager.widget.ViewPager
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.reshetplayer.defaultplayer.player.ReshetPlayerViewI
import com.applicaster.reshetplayer.kantar.KANTAR_ATTRIBUTE_STREAM_KEY
import com.applicaster.reshetplayer.kantar.KantarPlayerAdapter
import com.applicaster.reshetplayer.kantar.VideoData
import com.applicaster.reshetplayer.kantar.kantarSensor
import com.applicaster.reshetplayer.playercontroller.*
import com.applicaster.util.OSUtil.getScreenHeight
import com.applicaster.util.OSUtil.getScreenWidth
import de.spring.mobile.Stream
import java.util.*


class ReshetPlayerView(context: Context, val playerView: ReshetPlayerViewI) : RelativeLayout(context), LifecycleObserver, VideoData {

    companion object {
        const val TAG = "ReshetPlayerView"
    }

    var mPlayable : Playable? = null

    val playerContainer: ViewGroup

    private var controllerType: ContollerType = ContollerType.basic

    /*kantar stream*/
    private var stream: Stream? = null

    private lateinit var presentSinglePageListener: ViewPager.OnPageChangeListener


    init {
        LayoutInflater.from(context)
                .inflate(R.layout.reshet_player_new, this, true)
        View.inflate(context, R.layout.reshet_player_new, null)

        this.playerContainer = findViewById(R.id.player_container)

        this.playerContainer.addView(playerView.getVideoView())

        mPlayable = playerView.getPlayable()

        setMediaController()

        getActivity()!!.lifecycle.addObserver(this)

        playerContainer.setOnClickListener {
            mCustomMediaController?.toggleMediaControllerState()
        }

        startVideo()

    }

    fun setPlayable(playable: Playable) {
        mPlayable = playable
        ArtimediaManager.init(playable, findViewById(R.id.ad_video_frame), playerView, object : ArtimediaListner {
            override fun requestPauseConent() = pauseVideo()
            override fun requestResumeContant() = startVideo()

        })

        updateMediaController()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.d(TAG, "activity onResume")
        onResumeFromBackground()
    }

    fun onResumeFromBackground(){
        if(isVisible(this) && isAttachedToWindow) {
            ArtimediaManager.resumeAd()
            if (!ArtimediaManager.isAdInProgress()) {
                startVideo()
            }
        }
    }

    fun onResumeFromPageSelector(){
        ArtimediaManager.resumeAd()
        if (!ArtimediaManager.isAdInProgress()) {
            startVideo()
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.d(TAG, "activity onPause")
        ArtimediaManager.pasueAd()

        pauseVideo()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.d(TAG, "activity onDestroy")
        playerView.stopPlayback()
       ArtimediaManager.relese()
    }

    fun startVideo() {
        mCustomMediaController?.show()
        playerView.start()

        ArtimediaManager.onVideoStarted()

        if (mPlayable!!.isLive()) {
            startKantarStream()
        }
    }

    fun pauseVideo() {
        playerView.pause()
        Log.d(TAG, "pausing video")
        ArtimediaManager.onVideoPause()
        stopKantarStream()
    }

    fun stopVideo() {
        playerView.stopPlayback()
        ArtimediaManager.onVideoStop()
    }


    private fun startKantarStream() {
        stopKantarStream()
        val atts: MutableMap<String, Any> = HashMap()
        atts[KANTAR_ATTRIBUTE_STREAM_KEY] = PluginParams.kantarAttributeStreamValue // mandatory
        if (kantarSensor != null) {
            stream = kantarSensor!!.track(KantarPlayerAdapter(this), atts)
        }
    }

    private fun stopKantarStream() {
        if (stream != null) {
            stream!!.stop()
            stream = null
        }
    }

    override fun getCurrentVideoDate(): Long {
        return playerView.currentDate ?: 0L
    }


    private fun getActivity(): AppCompatActivity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    var mCustomMediaController: APMediaControllerI? = null

    protected fun setMediaController() {

        mCustomMediaController = findViewById<ReshetPlayerMediaControllerNew>(R.id.reshet_media_controller_new)

       updateMediaController()

        mCustomMediaController!!.initView()


    }

    fun setControllerType(controllerType: ContollerType) {
        this.controllerType = controllerType
        mCustomMediaController?.setControllerType(controllerType)
    }

    fun updateMediaController(){
        mCustomMediaController!!.setDefaultVisibility()
        mCustomMediaController!!.setPlayer(playerView)
        mCustomMediaController!!.setIsLive(mPlayable?.isLive() ?: false)
        mCustomMediaController!!.setPlayableItem(mPlayable)
        mCustomMediaController!!.setFullScreenCallback(fullscreenCallback)
        mCustomMediaController!!.setVolumeCallback(setVolumeCallback)
        mCustomMediaController!!.setControllerType(controllerType)
    }

    private var fullscreenCallback: FullscreenCallback? = null

    fun setFullScreenCallBack(fullscreenCallback: FullscreenCallback){
        this.fullscreenCallback = fullscreenCallback
        updateMediaController()
    }

    private var setVolumeCallback: SetVolumeCallback? = null

    fun setVolumeCallback(volumeCallback: SetVolumeCallback) {
        this.setVolumeCallback = volumeCallback
        updateMediaController()
    }

    fun onStopPlayback(){
        ArtimediaManager.onVideoStop()
        ArtimediaManager.relese()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this.onPause()

        val viewPager = this.findParent(ViewPager::class.java)
        if (viewPager != null) {
            viewPager.removeOnPageChangeListener(presentSinglePageListener)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.onResume()

        val viewPager = this.findParent(ViewPager::class.java)
        if (viewPager != null) {
            // If the component is on a page in a ViewPager, it should only be rendered, when it's page is selected
            presentSinglePageListener = viewPager.presentSinglePageListener(this, this)
            viewPager.addOnPageChangeListener(presentSinglePageListener)
        }
    }
}

fun View.removeFromParent(){
    (this.parent as? ViewGroup)?.removeView(this)
}

@Suppress("UNCHECKED_CAST")
fun <T> View.findParent(classType: Class<T>): T? {
    var parent = this.parent
    while (parent != null && (parent.javaClass != classType))
        parent = parent.parent
    return parent as? T
}

fun ViewPager.horizontallyInPage(v: View, position: Int): Boolean {
    val outArray: IntArray = intArrayOf(0, 0)
    this.getLocationOnScreen(outArray)
    for (i in 0..(this.childCount - 1)) {
        val c = this.getChildAt(i)
        //val defaultIndex: Int = (this.adapter as? TabSwipePagerAdapter)?.defaultIndex ?: 0
        val defaultIndex = 1
        if (v.isChildOf(c)) {
            return (c.x.toInt() == (position - defaultIndex) * this.width)
        }
    }
    return false
}

fun View.isChildOf(candidateParent: View): Boolean {
    var parent = this.parent
    while (parent != null && parent != candidateParent)
        parent = parent.parent
    return parent == candidateParent
}

fun ViewPager.presentSinglePageListener(view: View, reshetPlayerView: ReshetPlayerView) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrollStateChanged(state: Int) = Unit

    override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        if (!horizontallyInPage(view, position)) {
            reshetPlayerView.onPause()
        } else {
            reshetPlayerView.onResumeFromPageSelector()
        }
    }
}

fun isVisible(view: View?): Boolean {
    if (view == null) {
        return false
    }
//    if (!view.isShown) {
//        return false
//    }
    val actualPosition = Rect()
    view.getGlobalVisibleRect(actualPosition)
    val screen = Rect(0, 0, getScreenWidth(view.context), getScreenHeight(view.context))
    return actualPosition.intersect(screen)
}