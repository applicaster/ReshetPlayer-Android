package com.applicaster.reshetplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.applicaster.player.defaultplayer.DefaultPlayerWrapper;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration;
import com.applicaster.plugin_manager.playersmanager.PlayerContract;

import java.util.Date;
import java.util.Map;

import static com.applicaster.player.Player.PLAYABLE_KEY;
import static com.applicaster.reshetplayer.RemoteKt.fetchServerTime;
import static com.applicaster.reshetplayer.helpers.COneLogicKt.isInOne;
import static com.applicaster.reshetplayer.helpers.PlayableHelperKt.getVideoStartTime;
import static com.applicaster.reshetplayer.helpers.ServerDeltaTimeHelperKt.getServerDeltaTime;

public class StartHere extends DefaultPlayerWrapper {

    @Override
    public void playInFullscreen(PlayableConfiguration configuration, int requestCode, Context context) {

        Map conf = getPluginConfigurationParams();

        PluginParams.INSTANCE.initParams(conf);

        Long videoStartTime = getVideoStartTime(getFirstPlayable());

//        fetchServerTime(PluginParams.INSTANCE.getServerTimeUrl());
        fetchServerTime("https://13tv.co.il/timestamp.php");

        if(videoStartTime != null && isInOne(
                new Date().getTime(),
                getServerDeltaTime(),
                videoStartTime,
                PluginParams.INSTANCE.getC1_cut_time().getHours(),
                PluginParams.INSTANCE.getC1_cut_time().getMinuits(),
                PluginParams.INSTANCE.getC1_window_length_time()))
        {
            Playable playable = getFirstPlayable();
            playable.setContentVideoUrl(PluginParams.INSTANCE.getLiveStreamUrl());

            this.getPlayableList().clear();
            this.getPlayableList().add(playable);
        }

        Intent intent = new Intent(context, ReshetPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(PLAYABLE_KEY, new Playable[]{getFirstPlayable()});

        if (requestCode != PlayerContract.NO_REQUEST_CODE && context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
    }

    private boolean zappCheckboxToBoolean(@Nullable String value) {

        if ("0".equalsIgnoreCase(value))
            return false;

        if ("1".equalsIgnoreCase(value))
            return true;

        // handle "true"/"false"
        return Boolean.parseBoolean(value);
    }

    private void getServerDelataTime(){

    }
}