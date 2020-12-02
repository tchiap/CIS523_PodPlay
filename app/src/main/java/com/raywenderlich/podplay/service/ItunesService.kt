package com.raywenderlich.podplay.service

import retrofit2.Call
import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// page 455
// Chapter 20 networking


interface ItunesService {

    @GET("/search?media=podcast")

    // 2
    fun searchPodcastByTerm(@Query("term") term: String): Call<PodcastResponse>


    // 3
    companion object {

        val instance: ItunesService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create<ItunesService>(ItunesService::class.java)  // 6
        }
    }
}
