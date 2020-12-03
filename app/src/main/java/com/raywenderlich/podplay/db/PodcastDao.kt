package com.raywenderlich.podplay.db

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

// page 535 - chapter 24

// 1
@Dao
interface PodcastDao {

  // 2
  @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
  fun loadPodcasts(): LiveData<List<Podcast>>


  @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
  fun loadPodcastsStatic(): List<Podcast>


  @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
  fun loadEpisodes(podcastId: Long): List<Episode>

  @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
  fun loadPodcast(url: String): Podcast?

  // 4
  @Insert(onConflict = REPLACE)
  fun insertPodcast(podcast: Podcast): Long

  // 5
  @Insert(onConflict = REPLACE)
  fun insertEpisode(episode: Episode): Long


  // page 551
  @Delete
  fun deletePodcast(podcast: Podcast)
}
