
package com.raywenderlich.podplay.adapter

import android.view.ViewGroup
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View

import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.R

import kotlinx.android.synthetic.main.episode_item.view.*
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData


class EpisodeListAdapter(
  private var episodeViewList: List<EpisodeViewData>?,
  private val episodeListAdapterListener: EpisodeListAdapterListener) :
  RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

  // page 585, 586 - Chapter 26

  interface EpisodeListAdapterListener {
    fun onSelectedEpisode(episodeViewData: EpisodeViewData)
  }

  class ViewHolder(
    v: View, private val episodeListAdapterListener: EpisodeListAdapterListener) :
    RecyclerView.ViewHolder(v) {


    // Page 586
    init {
      v.setOnClickListener {
        episodeViewData?.let {
          episodeListAdapterListener.onSelectedEpisode(it)
        }
      }
    }


    // pasted from page 524
    var episodeViewData: EpisodeViewData? = null
    val titleTextView: TextView = v.titleView
    val descTextView: TextView = v.descView
    val durationTextView: TextView = v.durationView
    val releaseDateTextView: TextView = v.releaseDateView
  }

  // Page 524
  override fun getItemCount(): Int {
    return episodeViewList?.size ?: 0
  }

  override fun onCreateViewHolder(parent: ViewGroup,
                                  viewType: Int): EpisodeListAdapter.ViewHolder {
    return ViewHolder(LayoutInflater.from(parent.context)
      .inflate(R.layout.episode_item, parent, false),
      episodeListAdapterListener)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val episodeViewList = episodeViewList ?: return

    val episodeView = episodeViewList[position]

    holder.episodeViewData = episodeView
    holder.titleTextView.text = episodeView.title
    holder.descTextView.text =  HtmlUtils.htmlToSpannable(episodeView.description ?: "")
    holder.durationTextView.text = episodeView.duration
    holder.releaseDateTextView.text = episodeView.releaseDate?.let { DateUtils.dateToShortDate(it) }

  }


}
