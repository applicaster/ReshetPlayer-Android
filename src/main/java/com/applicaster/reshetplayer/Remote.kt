package com.applicaster.reshetplayer

import android.util.Log
import com.applicaster.reshetplayer.helpers.setSeverDelatTime
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


val serverTimeService : ServerTimeService by lazy {
    Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://13tv.co.il/")
            .build()
            .create(ServerTimeService::class.java)
}

interface ServerTimeService {
    @GET
    fun time(@Url url: String): Call<ResponseBody>
}

fun fetchServerTime(serverUrl: String) {
    serverTimeService.time(serverUrl).enqueue(object : Callback<ResponseBody> {
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            // do nothing
            Log.d("error", t.message)
        }

        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if(response.isSuccessful){
                setSeverDelatTime(Date().time - parseServerDate(response.body()?.string() ?: ""))
            }
        }

    })
}

val simpleDateFormatServerTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

fun parseServerDate(str: String) : Long {
    return try {
        simpleDateFormatServerTime.parse(str).time
    } catch (e: ParseException) {
        0
    }
}