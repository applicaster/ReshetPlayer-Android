package com.applicaster.reshetplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.applicaster.player.defaultplayer.DefaultPlayerWrapper;
import com.applicaster.plugin_manager.playersmanager.Playable;
import com.applicaster.plugin_manager.playersmanager.PlayableConfiguration;
import com.applicaster.plugin_manager.playersmanager.PlayerContract;

import java.util.Map;

import static com.applicaster.player.Player.PLAYABLE_KEY;

public class StartHere extends DefaultPlayerWrapper {

    static final String CONF_SITE_KEY = "site_key";
    static final String CONF_SHOW_ADS_ON_PAYED = "show_ads_on_payed";

    @Override
    public void playInFullscreen(PlayableConfiguration configuration, int requestCode, Context context) {

        Map conf = getPluginConfigurationParams();

        String siteKey = (String) conf.get(CONF_SITE_KEY);
        boolean showAdsOnPayed = zappCheckboxToBoolean((String) conf.get(CONF_SHOW_ADS_ON_PAYED));

        Intent intent = new Intent(context, ReshetPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(PLAYABLE_KEY, new Playable[]{getFirstPlayable()});
        intent.putExtra(CONF_SITE_KEY, siteKey);
        intent.putExtra(CONF_SHOW_ADS_ON_PAYED, showAdsOnPayed);

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
}