package com.applicaster.reshetplayer

import android.util.Log
import com.applicaster.util.OSUtil
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

val mOVidiusService : OVidiusService by lazy {
    Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(PluginParams.ovidiusUrl)
            .build()
            .create(OVidiusService::class.java)
}

enum class ServerType(val serverType: String) {
    PHONE ("appandroid"),
    TABLET ("androidtablet")
}


interface OVidiusService {
    @GET("api/getlink/getVideoById")
    fun getVideoById(
            @Query("userId") userId: String,
            @Query("videoId") videoId: String,
            @Query("serverType") serverType: ServerType
    ): Call<List<OvidousModel>>

    @GET("api/getlink/getVideoByFileName")
    fun getVideoByName(
            @Query("userId") userId: String,
            @Query("videoName") fileName: String,
            @Query("serverType") serverType: ServerType
    ): Call<List<OvidousModel>>

    @GET("api/getlink")
    fun getLink(
            @Query("userId") userId: String,
            @Query("cdnName") cdnName: String,
            @Query("ch") chanelName: String,
            @Query("serverType") serverType: ServerType
    ): Call<List<OvidousModel>>
}


data class OvidousModel(
        @SerializedName("Bitrates")
        val bitretes: String,
        @SerializedName("Token")
        val token: String,
        @SerializedName("ServerType")
        val serverType: String,
        @SerializedName("MediaRoot")
        val mediaRoot: String,
        @SerializedName("ProtocolType")
        val protocolType: String,
        @SerializedName("ServerAddress")
        val serverAddress: String,
        @SerializedName("SocketVodAddress")
        val socketVodAddress: String,
        @SerializedName("SocketLiveAddress")
        val socketLiveAddress: String,
        @SerializedName("MediaFile")
        val mediaFile: String,
        @SerializedName("StreamingType")
        val streamingType: String,
        @SerializedName("Duration")
        val duration: Int,
        @SerializedName("CanClientSkip")
        val canClientSkip: Boolean,
        @SerializedName("RefID")
        val RefId: String,
        @SerializedName("MediaFileID")
        val MediaFileId: String,
        @SerializedName("VideoWidth")
        val videoWidth: String,
        @SerializedName("VideoHeight")
        val videoHeight: String,
        @SerializedName("RefLink")
        val RefLink: String,
        @SerializedName("CampaignId")
        val campaignID: Int,
        @SerializedName("ShowTitle")
        val showTitle: String,
        @SerializedName("ShowID")
        val showId: String,
        @SerializedName("Tags")
        val tags: String,
        @SerializedName("ShortDescription")
        val shortDescription: String,
        @SerializedName("LongDescription")
        val longDescription: String,
        @SerializedName("ThumbNail")
        val thumbnail: String
)

interface CallbackResponseOVidius{
    fun onSucceed(result: String)
    fun onError()
}

fun getVideoSrc(videoName: String, callback: CallbackResponseOVidius) {
    mOVidiusService.getVideoByName(getUserID(), videoName, getServerType()).enqueue(object : Callback<List<OvidousModel>> {
        override fun onFailure(call: Call<List<OvidousModel>>, t: Throwable) {
            // do nothing
            Log.d("error", t.message)
            callback.onError()
        }

        override fun onResponse(call: Call<List<OvidousModel>>, response: Response<List<OvidousModel>>) {
            if(response.isSuccessful){
                response.body()?.firstOrNull()?.let {

                    callback.onSucceed(getSrcFromOvidousModel(it))

                } ?: callback.onError()
            } else {
                callback.onError()
            }
        }

    })
}

fun getLiveSrc(callback: CallbackResponseOVidius) {
    mOVidiusService.getLink(getUserID(), getCdnName(), getChanelName(),  getServerType()).enqueue(object : Callback<List<OvidousModel>> {
        override fun onFailure(call: Call<List<OvidousModel>>, t: Throwable) {
            // do nothing
            Log.d("error", t.message)
            callback.onError()
        }

        override fun onResponse(call: Call<List<OvidousModel>>, response: Response<List<OvidousModel>>) {
            if(response.isSuccessful){
                response.body()?.firstOrNull()?.let {

                    callback.onSucceed(getSrcFromOvidousModel(it))

                } ?: callback.onError()
            } else {
                callback.onError()
            }
        }

    })
}

fun getServerType() = when (OSUtil.isTablet()) {
        true -> ServerType.TABLET
        false -> ServerType.PHONE
    }

fun getUserID() = PluginParams.ovidiusUserID

fun getCdnName() = ""

fun getChanelName() = ""

fun getSrcFromOvidousModel(model: OvidousModel) : String{
    val fileNameArray = model.mediaFile.substringBeforeLast(".")
    val fileEnding = "." + model.mediaFile.substringAfterLast(".")
    val extendFileName = fileNameArray + model.bitretes + fileEnding
    return model.protocolType + model.serverAddress + model.mediaRoot + extendFileName + model.streamingType + model.token
}


