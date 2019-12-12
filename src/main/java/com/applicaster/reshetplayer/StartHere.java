package com.applicaster.reshetplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.applicaster.analytics.AnalyticsAgentUtil;
import com.applicaster.player.defaultplayer.DefaultPlayerWrapper;
import com.applicaster.player.defaultplayer.gmf.GmfPlayer;
import com.applicaster.player.defaultplayer.gmf.layeredvideo.PlaybackControlLayer;
import com.applicaster.plugin_manager.hook.ApplicationLoaderHookUpI;
import com.applicaster.plugin_manager.hook.HookListener;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration;
import com.applicaster.plugin_manager.playersmanager.PlayerContract;
import com.applicaster.util.UrlSchemeUtil;
import com.applicaster.util.ui.ShareDialog;

import java.util.Date;
import java.util.Map;

import static com.applicaster.player.Player.PLAYABLE_KEY;
import static com.applicaster.reshetplayer.RemoteKt.setServerDeltaTime;
import static com.applicaster.reshetplayer.ReshetPlayer.NEED_TO_SEEK_START_TIME;
import static com.applicaster.reshetplayer.helpers.COneLogicKt.isInOne;
import static com.applicaster.reshetplayer.helpers.PlayableHelperKt.getVideoStartTime;
import static com.applicaster.reshetplayer.helpers.ServerDeltaTimeHelperKt.getServerDeltaTime;

public class StartHere extends DefaultPlayerWrapper implements ApplicationLoaderHookUpI {

    @Override
    public void playInFullscreen(PlayableConfiguration configuration, int requestCode, Context context) {

//        Long videoStartTime = getVideoStartTime(getFirstPlayable());

//        getFirstPlayable().setContentVideoUrl("https://reshet-live.ctedgecdn.net/13tv-desktop/r13.m3u8?DVR?");

        Playable playable = getFirstPlayable();

        boolean shouldSeeKToVideoStartDate = false;
        if(getVideoStartTime(playable) != null && isInOne(
                new Date().getTime(),
                getServerDeltaTime(),
                getVideoStartTime(playable),
                PluginParams.INSTANCE.getC1_cut_time().getHours(),
                PluginParams.INSTANCE.getC1_cut_time().getMinuits(),
                PluginParams.INSTANCE.getC1_window_length_time()))
        {
            playable.setContentVideoUrl(PluginParams.INSTANCE.getLiveStreamUrl());
            shouldSeeKToVideoStartDate = true;
        }

        Intent intent = new Intent(context, ReshetPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(PLAYABLE_KEY, new Playable[]{getFirstPlayable()});
        intent.putExtra(NEED_TO_SEEK_START_TIME, shouldSeeKToVideoStartDate);

        if (requestCode != PlayerContract.NO_REQUEST_CODE && context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    public void playInline(PlayableConfiguration configuration) {
        this.configuration = configuration;

        Playable playable = getFirstPlayable();

//        playable.setContentVideoUrl("https://reshet-live.ctedgecdn.net/13tv-desktop/r13.m3u8?DVR=true");

        // trying to parse ads from playable extension if it is possible
        playable = parseAdsFromPlayableIfPossible(playable);

        if (playable == mCurrentPlayable && mCurrentState == State.Playing) {
            this.setPlaybackPosition(configuration);
        }
        else {
            mCurrentPlayable = playable;
            setPlayerState(State.LoadingPlayable);
            loadPlayable();
        }
    }

    @Override
    protected void setupPlayer() {
       
        setPlayerState(State.Playing);

        if (mGmfPlayer != null) {
            mGmfPlayer.release();
        }


        mGmfPlayer = new GmfPlayer((Activity) getContext(), mVideoCellView, mCurrentVideo, mCurrentImaAdUrl, mCurrentPlayable, eventEmitter);
        mGmfPlayer.setFullscreenCallback(new PlaybackControlLayer.FullscreenCallback() {
            @Override
            public void onGoToFullscreen(int currPosition) {
                StartHere.this.playInFullscreen(configuration, 0, getContext());
            }

            @Override
            public void onReturnFromFullscreen(boolean isVideoEnded) {
                if(isVideoEnded)
                    removeInline(videoContainerView);
            }
        });

        mGmfPlayer.addActionButton(
                ContextCompat.getDrawable(getContext(), com.applicaster.R.drawable.ic_action_share),
                "Share",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mGmfPlayer != null)
                            mGmfPlayer.pause();
                        // TODO switch to: GenericShareUtils.shareNative(context, holder, analyticName, extraAnalyticsParam);
                        ShareDialog.showCaptureVideoShareDialog((Activity) getContext(), getFirstPlayable().getPublicPageURL() + "?" + UrlSchemeUtil.addSnippestParams(0, 0), getFirstPlayable().getPlayableName(), null);
                    }
                }
        );
        mGmfPlayer.start();
        mVideoCellView.setStreamPlayer(mGmfPlayer);
        mVideoCellView.setPlayerContract(this);

        Map<String, String> params = mCurrentPlayable.getAnalyticsParams();
        params.put(AnalyticsAgentUtil.VIEW,AnalyticsAgentUtil.INLINE_PLAYER);
        AnalyticsAgentUtil.logTimedEvent(mCurrentPlayable.isLive() ? AnalyticsAgentUtil.PLAY_CHANNEL : AnalyticsAgentUtil.PLAY_VOD_ITEM, params);
        this.setPlaybackPosition(configuration);
    }

    @Override
    public void executeOnApplicationReady(Context context, HookListener listener) {
        initPluginParams();

        String serverUrl = PluginParams.INSTANCE.getServerTimeUrl();

        setServerDeltaTime(serverUrl, new CallbackResponse() {
            @Override
            public void onSucceed() {
                listener.onHookFinished();
            }

            @Override
            public void onError() {
                listener.onHookFinished();
            }
        });
    }



    @Override
    public void executeOnStartup(Context context, HookListener listener) {
        listener.onHookFinished();
    }

    void initPluginParams(){
        Map conf = getPluginConfigurationParams();

        PluginParams.INSTANCE.initParams(conf);
    }
}