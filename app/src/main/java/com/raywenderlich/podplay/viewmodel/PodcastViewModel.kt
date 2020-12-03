package com.raywenderlich.podplay.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.AndroidViewModel
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast

import android.app.Application
import com.raywenderlich.podplay.util.DateUtils

import com.raywenderlich.podplay.repository.PodcastRepo

import androidx.lifecycle.Transformations

import com.raywenderlich.podplay.viewmodel.SearchViewModel.PodcastSummaryViewData
import java.util.*





class PodcastViewModel(application: Application) : AndroidViewModel(application) {

  // page 494
  var podcastRepo: PodcastRepo? = null
  var activePodcastViewData: PodcastViewData? = null

  // page 543
  private var livePodcastData: LiveData<List<PodcastSummaryViewData>>? = null



  var activeEpisodeViewData: EpisodeViewData? = null


  private var activePodcast: Podcast? = null // p542

  // page 494
  data class PodcastViewData(
    var subscribed: Boolean = false,
    var feedTitle: String? = "",
    var feedUrl: String? = "",
    var feedDesc: String? = "",
    var imageUrl: String? = "",
    var episodes: List<EpisodeViewData>
  )

  // page 494
  data class EpisodeViewData (
    var guid: String? = "",
    var title: String? = "",
    var description: String? = "",
    var mediaUrl: String? = "",
    var releaseDate: Date? = null,
    var duration: String? = "",
    var isVideo: Boolean = false
  )



  // page 570
  fun setActivePodcast(feedUrl: String, callback: (PodcastSummaryViewData?) -> Unit) {

    val repo = podcastRepo ?: return
    repo.getPodcast(feedUrl) {

      if (it == null) {
        callback(null)
      } else {

        activePodcastViewData = podcastToPodcastView(it)
        activePodcast = it
        callback(podcastToSummaryView(it))
      }
    }
  }

  // p 543
  fun getPodcasts(): LiveData<List<PodcastSummaryViewData>>? {

    val repo = podcastRepo ?: return null

    // 1
    if (livePodcastData == null) {

        // 2
        val liveData = repo.getAll()

        // 3
        livePodcastData = Transformations.map(liveData) { podcastList ->
            podcastList.map { podcast ->
                podcastToSummaryView(podcast)
            }
        }

    }

    return livePodcastData
  }

  // chapter 22:  Podcast Details
  // 1
  fun getPodcast(podcastSummaryViewData: PodcastSummaryViewData, callback: (PodcastViewData?) ->
  Unit) {

    // 2
    val repo = podcastRepo ?: return

    val feedUrl = podcastSummaryViewData.feedUrl ?: return


    repo.getPodcast(feedUrl) {
      it?.let {
        it.feedTitle = podcastSummaryViewData.name ?: ""
        it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
        // 7
        activePodcastViewData = podcastToPodcastView(it)

        activePodcast = it

        // 8 callback
        callback(activePodcastViewData)
      }
    }
  }


  // page 542
  fun saveActivePodcast() {
    val repo = podcastRepo ?: return

    activePodcast?.let {
      repo.save(it)
    }
  }


  // page 494
  private fun episodesToEpisodesView(episodes: List<Episode>):
          List<EpisodeViewData> {

    return episodes.map {
        val isVideo = it.mimeType.startsWith("video")
        EpisodeViewData(it.guid, it.title, it.description, it.mediaUrl,
            it.releaseDate, it.duration, isVideo)
    }
  }

  // page 542
  private fun podcastToSummaryView(podcast: Podcast): PodcastSummaryViewData {

    return PodcastSummaryViewData(
        podcast.feedTitle,
        DateUtils.dateToShortDate(podcast.lastUpdated),
        podcast.imageUrl,
        podcast.feedUrl)
  }




  // ch 24, page 551
  fun deleteActivePodcast() {
    val repo = podcastRepo ?: return
    activePodcast?.let {
        repo.delete(it)
    }
  }

  // page 495
  private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
    return PodcastViewData(
        podcast.id != null,
        podcast.feedTitle,
        podcast.feedUrl,
        podcast.feedDesc,
        podcast.imageUrl,
        episodesToEpisodesView(podcast.episodes)
    )
  }



}
