package com.applicaster.reshetplayer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.RelativeLayout;

import com.applicaster.analytics.AnalyticsAgentUtil;
import com.applicaster.app.APProperties;
import com.applicaster.atom.model.APAtomEntry;
import com.applicaster.model.APChannel;
import com.applicaster.model.APURLPlayable;
import com.applicaster.model.APVodItem;
import com.applicaster.player.Player;
import com.applicaster.player.controller.APLightFavoritesMediaController;
import com.applicaster.player.controller.APLightMediaController;
import com.applicaster.player.controller.APMediaController;
import com.applicaster.player.controller.APMediaControllerI;
import com.applicaster.player.controller.APSocialBarData;
import com.applicaster.player.wrappers.PlayerViewWrapper;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.reshetplayer.kantar.KantarPlayerAdapter;
import com.applicaster.util.AppData;
import com.applicaster.util.OSUtil;
import com.applicaster.util.StringUtil;
import com.applicaster.util.ui.APVideoViewWrapper;

import net.artimedia.artisdk.api.AMContentState;
import net.artimedia.artisdk.api.AMEventListener;
import net.artimedia.artisdk.api.AMEventType;
import net.artimedia.artisdk.api.AMInitParams;
import net.artimedia.artisdk.api.AMSDK;
import net.artimedia.artisdk.api.AMSDKAPI;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.spring.mobile.Stream;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.applicaster.reshetplayer.OvidiusServiceKt.getLiveSrc;
import static com.applicaster.reshetplayer.OvidiusServiceKt.getVideoSrc;
import static com.applicaster.reshetplayer.helpers.PlayableHelperKt.getVideoStartTime;
import static com.applicaster.reshetplayer.helpers.PlayableHelperKt.isDvr;
import static com.applicaster.reshetplayer.kantar.KantarSensorKt.KANTAR_ATTRIBUTE_STREAM_KEY;
import static com.applicaster.reshetplayer.kantar.KantarSensorKt.getKantarSensor;

public class ReshetPlayer extends Player implements AMEventListener {

    public static final String TAG = ReshetPlayer.class.getSimpleName();

    private final static String IS_SEEK_TO_VIDEO_START_KEY = "is seek to video start";

    public final static String NEED_TO_SEEK_START_TIME = "need to seek video start time";

    private AMSDKAPI api;
    private Subscription positionTimer;
    private boolean adInProgress = false;
    private boolean mAdInitialized = false; //determined if the SDK was initialized successfully

    private Stream stream;

    boolean isSeekToVideoStartTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isSeekToVideoStartTime = savedInstanceState.getBoolean(IS_SEEK_TO_VIDEO_START_KEY);
        }

        View v = findViewById(R.id.ad_video_frame);

        String artimediaSiteName = PluginParams.INSTANCE.getArtimediaSiteName();
        boolean showAdsOnPayed = PluginParams.INSTANCE.getShowAdsOnPayed();

        // prepare json object
        JSONObject params = new JSONObject();
        try {
            params.put("siteKey", artimediaSiteName);
            params.put("videoID", playable.getPlayableId());
            params.put("isLive", playable.isLive() || isDvr(playable));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        api = AMSDK.getVideoAdvAPI();

        api.registerEvent(AMEventType.EVT_INIT_COMPLETE, this);
        api.registerEvent(AMEventType.EVT_PAUSE_REQUEST, this);
        api.registerEvent(AMEventType.EVT_RESUME_REQUEST, this);
        api.registerEvent(AMEventType.EVT_LINEAR_AD_START, this);
        api.registerEvent(AMEventType.EVT_LINEAR_AD_PAUSE, this);
        api.registerEvent(AMEventType.EVT_LINEAR_AD_RESUME, this);
        api.registerEvent(AMEventType.EVT_LINEAR_AD_STOP, this);
        api.registerEvent(AMEventType.EVT_AD_MISSED, this);
        api.registerEvent(AMEventType.EVT_AD_SHOW, this);
        api.registerEvent(AMEventType.EVT_AD_HIDE, this);
        api.registerEvent(AMEventType.EVT_SESSION_END, this);
        api.registerEvent(AMEventType.EVT_AD_CLICK, this);

        /*
         * Set of conditions to check whether we should play ads or skip them
         */

        // skip playing ads if item is not free and configured correctly on plugin
        if (playable instanceof APVodItem && !((APVodItem) playable).isFree() && !showAdsOnPayed) {
            return;
        }

        // skip playing ads if connected to a Cast device
        if (castPlugin != null && castPlugin.shouldPlayWithCast()) {
            return;
        }

        api.init(new AMInitParams(v, params));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean(IS_SEEK_TO_VIDEO_START_KEY, isSeekToVideoStartTime);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemLoaded(Playable loadedPlayable) {
        // replace our local playable with the loaded one.
        this.playable = loadedPlayable;
        if (!playable.isLive()) {
            String videoId = playable.getPlayableId();
            if (videoId != null && !videoId.isEmpty()) {
                getVideoSrc(videoId, new CallbackResponseOVidius() {
                    @Override
                    public void onError(@NotNull String e) {
                        Log.e("onError",e);
                    }

                    @Override
                    public void onSucceed(@NotNull String result) {
                        ((APAtomEntry.APAtomEntryPlayable) playable).getEntry().getContent().src = result;
                        playable.setContentVideoUrl(result);
                        streamUrl = playable.getContentVideoURL();
                        // Start a login process in case there's a login plugin present
                        processPaidItems(playable, videoView, ReshetPlayer.this, storeFrontHandler);
                    }

                });
            }
        } else {

            getLiveSrc(new CallbackResponseOVidius() {
                @Override
                public void onSucceed(@NotNull String result) {
                    if(playable instanceof APChannel){((APChannel) playable).setStream_url(result);}
                    playable.setContentVideoUrl(result);
                    streamUrl = playable.getContentVideoURL();
                    // Start a login process in case there's a login plugin present
                    processPaidItems(playable, videoView, ReshetPlayer.this, storeFrontHandler);
                }

                @Override
                public void onError(@NotNull String e) {
                    Log.e("onError",e);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (api != null && adInProgress) {
            api.resumeAd();
        }
        Log.d(TAG, "activity onResume");


    }

    @Override
    public void onPause() {
        super.onPause();
        if (api != null && adInProgress) {
            api.pauseAd();
        }
        Log.d(TAG, "activity onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissTimer();
        if (api != null) {
            api.destroy();
        }

        Log.d(TAG, "sdk destroyed");
    }

    @Override
    public void onAMSDKEvent(AMEventType amEventType, Object o) {

        Log.d(TAG, amEventType.name());

        switch (amEventType) {

            case EVT_INIT_COMPLETE:
                //play video after init finished
                if ((Boolean) o) {
                    Log.d(TAG, "sdk initialized");
                    mAdInitialized = (Boolean) o;
                }
                break;
            case EVT_PAUSE_REQUEST:
                pauseVideo();
                playerContainer.removeView(videoView);
                break;
            case EVT_RESUME_REQUEST:
                // move to EVT_LINEAR_AD_STOP
                //  adInProgress = false;
                playerContainer.addView(videoView, 0);
                startVideo();
                break;
            case EVT_AD_SHOW:
                Log.d(TAG, "ad is in progress");
                break;
            case EVT_AD_MISSED:
                adInProgress = false;
                break;
            case EVT_LINEAR_AD_START: // start to play ads
                adInProgress = true;
                break;
            case EVT_LINEAR_AD_STOP:  // stop playing ads
                adInProgress = false;
                break;
            case EVT_SESSION_END:
                adInProgress = false;
                break;
        }
    }

    @Override
    public void startVideo() {


        if (!adInProgress && !videoView.isPlaying()) {
            super.startVideo();

            Log.d(TAG, "starting video");

            if (mAdInitialized && api != null && (positionTimer == null || positionTimer.isUnsubscribed())) {

                api.updateVideoState(AMContentState.VIDEO_STATE_PLAY);

                positionTimer = Observable
                        .interval(1, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .doOnNext((sec) -> {

                            float pos = (float) Math.ceil(getCurrentPosition() / 1000);

                            if (api != null && pos > 0) {
                                api.updateVideoTime(pos);
                            }

//                            Log.d(TAG, "sending position: " + pos);
//                            Log.d(TAG, "sending date: " + videoView.getCurrentDate());
//                            Long currentVideoDate = videoView.getCurrentDate();
//                            if(currentVideoDate != null){
//                                Date currentVideoDateDate = new Date(currentVideoDate);
//                                Log.d(TAG, "sending position from date: " + videoView.getPositionFromDate(currentVideoDateDate));
//                            }
                        }).subscribe();
            }
        }


        if (playable.isLive() || isDvr(playable)) {
            startKantarStream();
        }

    }

    @Override
    public void pauseVideo() {
        super.pauseVideo();

        Log.d(TAG, "pausing video");

        dismissTimer();
        api.updateVideoState(AMContentState.VIDEO_STATE_PAUSE);

        stopKantarStream();
    }

    @Override
    public void stopVideo() {
        super.stopVideo();

        Log.d(TAG, "stopping video");

        api.updateVideoState(AMContentState.VIDEO_STATE_STOP);
    }

    @Override
    protected int getLayout() {
        return OSUtil.getLayoutResourceIdentifier("reshet_player");
    }

    private void dismissTimer() {

        if (positionTimer != null && !positionTimer.isUnsubscribed()) {

            positionTimer.unsubscribe();
            Log.d(TAG, "dismissed timer");
        }
    }

    private boolean isSeekToVideoStartTimeNeeded() {
        return getIntent().getBooleanExtra(NEED_TO_SEEK_START_TIME, false);
    }

    private void seekToVideoStartTime() {
        Long videoStartTime = getVideoStartTime(playable);

        if (videoStartTime != null) {
            Integer position = videoView.getPositionFromDate(new Date(videoStartTime));
            if (position != null) {
                Log.d(TAG, "seeking to " + position);
                videoView.seekTo(position);
                videoCurrentPosition = position;
            } else {
                Log.d(TAG, "seekToVideoStartTime: return null");
            }
        }
    }

    APMediaControllerI mCustomMediaController;

    protected void setMediaController() {
        if (playerConfig.showNativeMediaController) {
            Log.d("Player", "playerConfig.showNativeMediaController==true");
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
        } else {
            Log.d("Player", "!playerConfig.showNativeMediaController");
            if (AppData.getProperty(APProperties.MEDIA_PLAYER_CONTROLLER, DEFAULT_PLAYER_CONTROLLER).equals(LIGHT_PLAYER_CONTROLLER)) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mCustomMediaController = new APLightMediaController(this, null);
                playerContainer.addView((APLightMediaController) mCustomMediaController, params);
            } else if (AppData.getProperty(APProperties.MEDIA_PLAYER_CONTROLLER, DEFAULT_PLAYER_CONTROLLER).equals(LIGHT_FAVORITES_PLAYER_CONTROLLER)) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mCustomMediaController = new APLightFavoritesMediaController(this, null);
                playerContainer.addView((APLightFavoritesMediaController) mCustomMediaController, params);
            } else {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mCustomMediaController = new APMediaController(this, null) {

                    @Override
                    public void show() {
                        if (isLive && isDvr(playable) == false) {
                            if (timerContainer != null) {
                                timerContainer.setVisibility(View.GONE);
                                seekbar.setVisibility(View.GONE);
                            }
                            if (seekbarContainer != null) {
                                seekbarContainer.setVisibility(View.GONE);
                                seekbar.setVisibility(View.GONE);
                            }
                        } else {
                            int currentPosition = this.getCurrentPosition();
                            int duration = player.getDuration();
                            if (!isSocialbarEnabled) {
                                seekbarContainer.setVisibility(View.VISIBLE);
                            }
                            seekbar.setVisibility(View.VISIBLE);
                            seekbar.setMax(duration);
                            seekbar.setProgress(currentPosition);
                            String currentTime = StringUtil.parseDuration("" + currentPosition);
                            elapsedTime.setText(currentTime);
                            String totalTimeStr = StringUtil.parseDuration("" + duration);
                            totalTime.setText(totalTimeStr);

                            ((RelativeLayout.LayoutParams) seekbar.getLayoutParams()).topMargin = -1 * seekbar.getThumbOffset() / 2;
                            startCurrentPositionTimer();
                        }

                        displayTopBarWithAnimation();

                    }
                };
                playerContainer.addView((APMediaController) mCustomMediaController, params);
            }
            mCustomMediaController.setDefaultVisibility();
            mCustomMediaController.setPlayer(videoView);
            mCustomMediaController.setIsLive(playable.isLive());
            mCustomMediaController.setPlayableItem(playable);
            mCustomMediaController.initView();
            setCustomMediaController(mCustomMediaController);
        }
    }

    @Override
    protected void finalizeOnPrepare(boolean isPreroll) {
        super.finalizeOnPrepare(isPreroll);

        Log.d(TAG, "finalizeOnPrepare: ");

        if (!isSeekToVideoStartTime && isSeekToVideoStartTimeNeeded()) {
            seekToVideoStartTime();
            isSeekToVideoStartTime = true;
        }
    }

    @Override
    public void playVideo(boolean isPreRollUrl) {
        super.playVideo(isPreRollUrl);

        if (isDvr(playable) && videoCurrentPosition > 0) {
            restorePosition();
        }
    }

    private void startKantarStream() {
        Map<String, Object> atts = new HashMap<String, Object>();
        atts.put(KANTAR_ATTRIBUTE_STREAM_KEY, PluginParams.INSTANCE.getKantarAttributeStreamValue()); // mandatory
        stream = getKantarSensor().track(new KantarPlayerAdapter(this), atts);
    }

    private void stopKantarStream() {
        if (stream != null) {
            stream.stop();
            stream = null;
        }
    }
}