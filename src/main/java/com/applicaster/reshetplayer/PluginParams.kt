package com.applicaster.reshetplayer

object PluginParams {

    var artimediaSiteName: String = ""
    var kantarSiteName: String = ""
    var showAdsOnPayed: Boolean = false
    var liveStreamUrl: String = ""
    var c1_cut_time: SimpleTime = SimpleTime(0, 0)
    var c1_window_length_time: Int = 0
    var serverTimeUrl: String = ""
    var playerVersion: String = "0"
    var playerName: String = "reshet player"
    var kantarAttributeStreamValue = "android/teststream"
    var ovidiusUrl = "https://13tv-api.oplayer.io/"
    var ovidiusUserID = "45E4A9FB-FCE8-88BF-93CC-3650C39DDF28"
    var ovidiusCdnName = "casttime"
    var ovidiusCh = "1"

    internal val CONF_ARTIMEDIA_SITE_KEY = "artimedia_site_key"
    internal val CONF__DANTAR_SITE_KEY = "kantar_site_key"
    internal val CONF_SHOW_ADS_ON_PAYED = "show_ads_on_payed"
    internal val CONF_LIVE_STREAM_URL = "live_stream_url"
    internal val CONF_C1_CUT_TIME = "c1_cut_time"
    internal val CONF_C1_WINDOW_LENGTH_TIME = "c1_window_length_time"
    internal val CONF_SERVER_TIME_URL = "server_time_url"
    internal val CONF_KANTAR_PLAYER_VERSION = "kantar_player_version"
    internal val CONF_KANTAR_PLAYER_NAME = "kantar_player_name"
    internal val CONF_KANTAR_ATRRIBUTE_STREAM_VALUE = "kantar_attribute_stream_value"
    internal val CONF_OVIDIUS_URL= "ovidius_url"
    internal val CONF_OVIDIUS_USER_ID= "ovidius_user_ID"
    internal val CONF_OVIDIUS_CDN_NAME= "ovidius_cdn_name"
    internal val CONF_OVIDIUS_CH= "ovidius_Ch"

    fun initParams(pluginConfiguration: MutableMap<Any?, Any?>) {
        kantarSiteName = pluginConfiguration[CONF__DANTAR_SITE_KEY].toString()
        artimediaSiteName = pluginConfiguration[CONF_ARTIMEDIA_SITE_KEY].toString()
        showAdsOnPayed = zappCheckboxToBoolean(pluginConfiguration.get(CONF_SHOW_ADS_ON_PAYED).toString())
        liveStreamUrl = pluginConfiguration[CONF_LIVE_STREAM_URL].toString()
        c1_cut_time = parseTime(pluginConfiguration[CONF_C1_CUT_TIME].toString())
        c1_window_length_time = pluginConfiguration[CONF_C1_WINDOW_LENGTH_TIME].toString().toIntOrNull() ?: 36
        serverTimeUrl = pluginConfiguration[CONF_SERVER_TIME_URL].toString()
        playerVersion = pluginConfiguration[CONF_KANTAR_PLAYER_VERSION].toString()
        playerName = pluginConfiguration[CONF_KANTAR_PLAYER_NAME].toString()
        kantarAttributeStreamValue = pluginConfiguration[CONF_KANTAR_ATRRIBUTE_STREAM_VALUE].toString()
        ovidiusUrl = pluginConfiguration[CONF_OVIDIUS_URL].toString()
        ovidiusUserID = pluginConfiguration[CONF_OVIDIUS_USER_ID].toString()
        ovidiusCdnName = pluginConfiguration[CONF_OVIDIUS_CDN_NAME].toString()
        ovidiusCh = pluginConfiguration[CONF_OVIDIUS_CH].toString()
    }

    private fun zappCheckboxToBoolean(value: String?): Boolean {

        if ("0".equals(value, ignoreCase = true))
            return false

        if ("1".equals(value, ignoreCase = true))
            return true

        // handle "true"/"false"
        return java.lang.Boolean.parseBoolean(value)

    }

    data class SimpleTime(val hours: Int, val minuits: Int)

    //receive Time in HH:MM
    private fun parseTime(time: String) = SimpleTime(
            time.split(':').getOrNull(0)?.toIntOrNull() ?: 0,
            time.split(':').getOrNull(1)?.toIntOrNull() ?: 0
    )
}

