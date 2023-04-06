
package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.adapter.PodcastListAdapter.PodcastListAdapterListener
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService

import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.*
import com.raywenderlich.podplay.R

import com.raywenderlich.podplay.ui.PodcastDetailsFragment.OnPodcastDetailsListener
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(), PodcastListAdapterListener, OnPodcastDetailsListener {


  // chapter 21 - finding podcasts
  // 480
  private val searchViewModel by viewModels<SearchViewModel>()
  private val podcastViewModel by viewModels<PodcastViewModel>()
  private lateinit var podcastListAdapter: PodcastListAdapter

  private lateinit var searchMenuItem: MenuItem

  // page 545
  override fun onSubscribe() {
    podcastViewModel.saveActivePodcast()
    supportFragmentManager.popBackStack()
  }

  // page 552
  override fun onUnsubscribe() {
    podcastViewModel.deleteActivePodcast()
    supportFragmentManager.popBackStack()
  }


  // page 620
  override fun onShowEpisodePlayer(episodeViewData: EpisodeViewData) {
    podcastViewModel.activeEpisodeViewData = episodeViewData
    showPlayerFragment()
  }





  // page 468
  override fun onCreateOptionsMenu(menu: Menu): Boolean {

    // 1
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_search, menu)

    // 2
    searchMenuItem = menu.findItem(R.id.search_item)
    val searchView = searchMenuItem.actionView as SearchView

    // page 553
    searchMenuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
        return true
      }
      override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
        showSubscribedPodcasts()
        return true
      }
    })


    // 3
    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

    // 4
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

    if (supportFragmentManager.backStackEntryCount > 0) {
      podcastRecyclerView.visibility = View.INVISIBLE
    }

    if (podcastRecyclerView.visibility == View.INVISIBLE) {
      searchMenuItem.isVisible = false
    }

    return true
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  // page 503
  override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

    // 1
    val feedUrl = podcastSummaryViewData.feedUrl ?: return

    // 2
    showProgressBar()

    // 3
    podcastViewModel.getPodcast(podcastSummaryViewData) {

      // 4
      hideProgressBar()

      if (it != null) {
        // 5
        showDetailsFragment()
      } else {

        // 6
        showError("Error loading feed $feedUrl")
      }
    }
  }

  // p 568
  private fun scheduleJobs() {

    // 1
    val constraints: Constraints = Constraints.Builder().apply {
      setRequiredNetworkType(NetworkType.CONNECTED)
      setRequiresCharging(true)
    }.build()

    // 2
    val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

    // 3
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.REPLACE, request)
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // set activity podcast
    setContentView(R.layout.activity_podcast)
    setupToolbar()

    setupViewModels()

    updateControls()
    setupPodcastListView()

    // intent
    handleIntent(intent)

    addBackStackListener()

    scheduleJobs()
  }


  // p 546
  private fun showSubscribedPodcasts()
  {

    // 1
    val podcasts = podcastViewModel.getPodcasts()?.value
    // 2
    if (podcasts != null) {
      toolbar.title = getString(R.string.subscribed_podcasts)

      podcastListAdapter.setSearchData(podcasts)
    }
  }


  //
  private fun performSearch(term: String) {

    showProgressBar()

    searchViewModel.searchPodcasts(term) { results ->
      hideProgressBar()
      toolbar.title = term
      podcastListAdapter.setSearchData(results)

    }
  }



  // page 470
  private fun handleIntent(intent: Intent) {
    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY) ?: return
      performSearch(query)

    }


    // page 570
    val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)

    if (podcastFeedUrl != null) {
      podcastViewModel.setActivePodcast(podcastFeedUrl) {

        it?.let { podcastSummaryView -> onShowDetails(podcastSummaryView) }

      }
    }


  }


  companion object {
    // page 500
    private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"

    // page 568
    private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"

    // page 619
    private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
  }






  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }


  private fun setupViewModels() {

    // p 480
    val service = ItunesService.instance
    searchViewModel.iTunesRepo = ItunesRepo(service)

    // page 525
    val rssService = FeedService.instance


    val db = PodPlayDatabase.getInstance(this)

    val podcastDao = db.podcastDao()

    podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
  }

  private fun setupPodcastListView() {
    podcastViewModel.getPodcasts()?.observe(this, Observer {
      if (it != null) {

        showSubscribedPodcasts()
      }
    })
  }



  // page 480
  private fun updateControls() {

    podcastRecyclerView.setHasFixedSize(true)

    val layoutManager = LinearLayoutManager(this)
    podcastRecyclerView.layoutManager = layoutManager

    val dividerItemDecoration = DividerItemDecoration(podcastRecyclerView.context, layoutManager.orientation)
    podcastRecyclerView.addItemDecoration(dividerItemDecoration)

    podcastListAdapter = PodcastListAdapter(null, this, this)
    podcastRecyclerView.adapter = podcastListAdapter
  }

  // page 501
  private fun showDetailsFragment() {
    // 1
    val podcastDetailsFragment = createPodcastDetailsFragment()

    // 2
    supportFragmentManager.beginTransaction().add(R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT).addToBackStack("DetailsFragment").commit()
    // 3
    podcastRecyclerView.visibility = View.INVISIBLE

    //4
    searchMenuItem.isVisible = false
  }


  // page 619
  private fun showPlayerFragment() {
    val episodePlayerFragment = createEpisodePlayerFragment()

    supportFragmentManager.beginTransaction().replace(R.id.podcastDetailsContainer, episodePlayerFragment, TAG_PLAYER_FRAGMENT).addToBackStack("PlayerFragment").commit()
    podcastRecyclerView.visibility = View.INVISIBLE

    searchMenuItem.isVisible = false
  }

  // page 505
  private fun addBackStackListener()
  {
    supportFragmentManager.addOnBackStackChangedListener {
      if (supportFragmentManager.backStackEntryCount == 0) {
        podcastRecyclerView.visibility = View.VISIBLE
      }
    }
  }

  private fun createEpisodePlayerFragment(): EpisodePlayerFragment {

    var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as
        EpisodePlayerFragment?

    if (episodePlayerFragment == null) {
      episodePlayerFragment = EpisodePlayerFragment.newInstance()
    }
    return episodePlayerFragment
  }

  // Page 481
  private fun hideProgressBar() {
    progressBar.visibility = View.INVISIBLE
  }



  // p 500
  private fun createPodcastDetailsFragment(): PodcastDetailsFragment {

    // 1
    var podcastDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

    // 2
    if (podcastDetailsFragment == null) {
      podcastDetailsFragment = PodcastDetailsFragment.newInstance()
    }

    return podcastDetailsFragment
  }

  // page 481
  private fun showProgressBar() {
    progressBar.visibility = View.VISIBLE
  }
  
  // page 502
  private fun showError(message: String) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton(getString(R.string.ok_button), null)
        .create()
        .show()
  }


}
