package com.applicaster.reshetplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.applicaster.model.APVodItem;
import com.applicaster.player.Player;
import com.applicaster.reshetplayer.kantar.KantarPlayerAdapter;
import com.applicaster.util.OSUtil;

import net.artimedia.artisdk.api.AMContentState;
import net.artimedia.artisdk.api.AMEventListener;
import net.artimedia.artisdk.api.AMEventType;
import net.artimedia.artisdk.api.AMInitParams;
import net.artimedia.artisdk.api.AMSDK;
import net.artimedia.artisdk.api.AMSDKAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.spring.mobile.Stream;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.applicaster.reshetplayer.kantar.KantarSensorKt.getKantarSensor;

public class ReshetPlayer extends Player implements AMEventListener {

    public static final String TAG = ReshetPlayer.class.getSimpleName();

    private AMSDKAPI api;
    private Subscription positionTimer;
    private boolean adInProgress = false;
    private  boolean mAdInitialized = false; //determined if the SDK was initialized successfully

    private Stream stream;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View v = findViewById(R.id.ad_video_frame);

        String artimediaSiteName = PluginParams.INSTANCE.getArtimediaSiteName();
        boolean showAdsOnPayed = PluginParams.INSTANCE.getShowAdsOnPayed();

        // prepare json object
        JSONObject params = new JSONObject();
        try {
            params.put("siteKey", artimediaSiteName);
            params.put("videoID", playable.getPlayableId());
            params.put("isLive", playable.isLive());
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
    public void onResume() {
        super.onResume();
        if( api != null && adInProgress) {
            api.resumeAd();
        }
        Log.d(TAG, "activity onResume");


    }

    @Override
    public void onPause() {
        super.onPause();
        if(api != null && adInProgress) {
            api.pauseAd();
        }
        Log.d(TAG, "activity onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissTimer();
        if(api != null) {
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

            if (mAdInitialized && api != null && ( positionTimer == null || positionTimer.isUnsubscribed())) {

                api.updateVideoState(AMContentState.VIDEO_STATE_PLAY);

                positionTimer = Observable
                        .interval(1, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .doOnNext((sec) -> {

                            float pos = (float) Math.ceil(getCurrentPosition() / 1000);

                            if (api != null && pos > 0) {
                                api.updateVideoTime(pos);
                            }

                            Log.d(TAG, "sending position: " + pos);

                        }).subscribe();
            }
        }

        if (startKantarOnlyOnLive()) {
            if(playable.isLive()) {
                startKantarStream();
            }
        } else {
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

    private boolean startKantarOnlyOnLive(){
        return true;
    }

    private void startKantarStream() {
        Map<String, Object> atts = new HashMap<String, Object>();
        atts.put("stream", "android/teststream"); // mandatory
        stream = getKantarSensor().track(new KantarPlayerAdapter(this), atts);
    }

    private void stopKantarStream() {
        if(stream != null){
            stream.stop();
            stream = null;
        }
    }
}