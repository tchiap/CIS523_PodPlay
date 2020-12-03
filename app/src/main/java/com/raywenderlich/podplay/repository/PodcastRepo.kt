package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData

import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.RssFeedResponse

// page 493
class PodcastRepo(private var feedService: FeedService, private var podcastDao: PodcastDao) {

  // page 493
  fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

    GlobalScope.launch {

      // page 548
      val podcast = podcastDao.loadPodcast(feedUrl)

      if (podcast != null) {
        podcast.id?.let {
          podcast.episodes = podcastDao.loadEpisodes(it)
          GlobalScope.launch(Dispatchers.Main) {
            callback(podcast)
          }
        }
      } else {

        // page 523
        feedService.getFeed(feedUrl) { feedResponse ->
          var podcast: Podcast? = null
          if (feedResponse != null) {
            podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
          }

          GlobalScope.launch(Dispatchers.Main) {
            callback(podcast)
          }
        }
      }
    }

  }

  fun getAll(): LiveData<List<Podcast>>
  {
    return podcastDao.loadPodcasts()
  }


  // page 559
  fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {

    // 1
    val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()

    // 2
    val podcasts = podcastDao.loadPodcastsStatic()

    // 3
    var processCount = podcasts.count()

    // 4
    for (podcast in podcasts) {
      getNewEpisodes(podcast) { newEpisodes ->


        // 6
        if (newEpisodes.count() > 0) {
          saveNewEpisodes(podcast.id!!, newEpisodes)

          updatedPodcasts.add(PodcastUpdateInfo(podcast.feedUrl, podcast.feedTitle, newEpisodes.count()))
        }

        // 7
        processCount--

        if (processCount == 0) {

          // 8
          callback(updatedPodcasts)
        }
      }
    }
  }

  // Chapter 557 - Podcast Subscriptions, Part 2
  private fun getNewEpisodes(localPodcast: Podcast, callBack: (List<Episode>) -> Unit) {

    // 1
    feedService.getFeed(localPodcast.feedUrl) { response ->


      if (response != null) {
        val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, response)
        remotePodcast?.let {

          // 3
          val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)


          // 4
          val newEpisodes = remotePodcast.episodes.filter { episode ->
            localEpisodes.find { episode.guid == it.guid } == null
          }

          // 5
          callBack(newEpisodes)
        }
      } else {
        callBack(listOf())
      }
    }
  }

  // page 559
  private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
    GlobalScope.launch {
      for (episode in episodes) {
        episode.podcastId = podcastId
        podcastDao.insertEpisode(episode)
      }
    }
  }


  // page 551
  fun delete(podcast: Podcast) {
    GlobalScope.launch {
      podcastDao.deletePodcast(podcast)
    }
  }

  // chapter 23, page 522
  private fun rssResponseToPodcast(feedUrl: String, imageUrl: String, rssResponse: RssFeedResponse): Podcast? {

    // 1
    val items = rssResponse.episodes ?: return null

    // 2
    val description = if (rssResponse.description == "") rssResponse.summary else rssResponse.description

    // 3
    return Podcast(null, feedUrl, rssResponse.title, description, imageUrl,
      rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items))
  }




  // page 540, 541
  fun save(podcast: Podcast) {
    GlobalScope.launch {
      // 1
      val podcastId = podcastDao.insertPodcast(podcast)

      // 2
      for (episode in podcast.episodes) {
        // 3
        episode.podcastId = podcastId
        podcastDao.insertEpisode(episode)
      }
    }
  }



  private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
    return episodeResponses.map {
      Episode(
        it.guid ?: "",
        null,
        it.title ?: "",
        it.description ?: "",
        it.url ?: "",
        it.type ?: "",
        DateUtils.xmlDateToDate(it.pubDate),
        it.duration ?: ""
      )
    }
  }

  class PodcastUpdateInfo (val feedUrl: String, val name: String, val newCount: Int)
}
