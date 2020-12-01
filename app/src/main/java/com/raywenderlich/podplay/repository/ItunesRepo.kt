
package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.PodcastResponse
import com.raywenderlich.podplay.service.PodcastResponse.ItunesPodcast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// 1
class ItunesRepo(private val itunesService: ItunesService) {

  // 2 - page 458
  fun searchByTerm(term: String, callBack: (List<ItunesPodcast>?) -> Unit) {

    // 3
    val podcastCall = itunesService.searchPodcastByTerm(term)

    podcastCall.enqueue(object : Callback<PodcastResponse> {
      override fun onFailure(call: Call<PodcastResponse>?, t: Throwable?) {

        // 6
        callBack(null)
      }


      // 7 - page 459
      override fun onResponse(call: Call<PodcastResponse>?, response: Response<PodcastResponse>?) {

        // 8
        val body = response?.body()
        callBack(body?.results)
      }
    })
  }
}
