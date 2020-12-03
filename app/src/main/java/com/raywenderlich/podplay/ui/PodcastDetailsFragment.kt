package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.adapter.EpisodeListAdapter.EpisodeListAdapterListener
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData


import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide

import kotlinx.android.synthetic.main.fragment_podcast_details.*




class PodcastDetailsFragment : Fragment(), EpisodeListAdapterListener {

  // page 525
  private lateinit var episodeListAdapter: EpisodeListAdapter

  // page 544
  private var listener: OnPodcastDetailsListener? = null
  // page 549
  private var menuItem: MenuItem? = null


  // page 621
  private val podcastViewModel: PodcastViewModel by activityViewModels()






  // page 497
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 1
    setHasOptionsMenu(true)
  }


  override fun onSelectedEpisode(episodeViewData: EpisodeViewData) {
    // p618 - replace
    listener?.onShowEpisodePlayer(episodeViewData)
  }


  // page 497
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_podcast_details, container, false)
  }

  // page 499
  companion object {
    fun newInstance(): PodcastDetailsFragment {
      return PodcastDetailsFragment()
    }
  }



  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setupControls()

    // page 499
    updateControls()
  }

  // page 544
  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnPodcastDetailsListener) {
      listener = context
    } else {
      throw RuntimeException(context.toString() + " must implement OnPodcastDetailsListener")
    }
  }

  override fun onStart() {
    super.onStart()
  }



  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)


    inflater.inflate(R.menu.menu_details, menu)

    // page 550
    menuItem = menu.findItem(R.id.menu_feed_action)
    updateMenuItem()
  }

  override fun onStop() {
    super.onStop()
  }


  override fun onOptionsItemSelected(item: MenuItem): Boolean {

    when (item.itemId) {
      R.id.menu_feed_action -> {
        podcastViewModel.activePodcastViewData?.feedUrl?.let {

          if (podcastViewModel.activePodcastViewData?.subscribed == true) {
            listener?.onUnsubscribe()
          } else {
            listener?.onSubscribe()
          }
        }

        return true
      }
      else ->
        return super.onOptionsItemSelected(item)
    }
  }

  // page 525
  private fun setupControls() {

    // 1
    feedDescTextView.movementMethod = ScrollingMovementMethod()

    // 2
    episodeRecyclerView.setHasFixedSize(true)

    val layoutManager = LinearLayoutManager(activity)
    episodeRecyclerView.layoutManager = layoutManager

    val dividerItemDecoration = DividerItemDecoration(episodeRecyclerView.context, layoutManager.orientation)
    episodeRecyclerView.addItemDecoration(dividerItemDecoration)


    // 3
    episodeListAdapter = EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)
    episodeRecyclerView.adapter = episodeListAdapter
  }

  // chapter 22: podcast details
  // page 498
  private fun updateControls() {
    val viewData = podcastViewModel.activePodcastViewData ?: return
    feedTitleTextView.text = viewData.feedTitle

    feedDescTextView.text = viewData.feedDesc

    // page 499
    activity?.let { activity ->
      Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
    }
  }

  // page 550
  private fun updateMenuItem() {

    // 1
    val viewData = podcastViewModel.activePodcastViewData ?: return

    // 2
    menuItem?.title = if (viewData.subscribed) getString(R.string.unsubscribe)
        else getString(R.string.subscribe)
  }


  // page 544
  interface OnPodcastDetailsListener {
    fun onSubscribe()

    // page 549
    fun onUnsubscribe()
    fun onShowEpisodePlayer(episodeViewData: EpisodeViewData)
  }
}
