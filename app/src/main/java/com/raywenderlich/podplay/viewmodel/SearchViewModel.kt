package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.PodcastResponse


class SearchViewModel(application: Application) : AndroidViewModel(application) {

  // ch 21 Fining Podcasts , page 474
  var iTunesRepo: ItunesRepo? = null

  // page 474
  data class PodcastSummaryViewData(
      var name: String? = "",
      var lastUpdated: String? = "",
      var imageUrl: String? = "",
      var feedUrl: String? = "")



  // page 474
  // 1
  fun searchPodcasts(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {

    // 2
    iTunesRepo?.searchByTerm(term) { results ->
        if (results == null) {
            // 3 callback
            callback(emptyList())

        } else {
            // 4
            val searchViews = results.map { podcast ->
                itunesPodcastToPodcastSummaryView(podcast)
            }
            // 5
            callback(searchViews)
        }
    }
  }


  private fun itunesPodcastToPodcastSummaryView(itunesPodcast: PodcastResponse.ItunesPodcast): PodcastSummaryViewData {

    // update on page 607 to artworkURL30
    return PodcastSummaryViewData(
        itunesPodcast.collectionCensoredName,
        DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
        itunesPodcast.artworkUrl100,
        itunesPodcast.feedUrl)
  }


}
