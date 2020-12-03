
package com.raywenderlich.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod

import com.raywenderlich.podplay.service.PodplayMediaCallback
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.raywenderlich.podplay.service.PodplayMediaService


import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R

import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*

class EpisodePlayerFragment : Fragment() {

  private val podcastViewModel: PodcastViewModel by activityViewModels()

  // page 580
  private lateinit var mediaBrowser: MediaBrowserCompat

  private var mediaControllerCallback: MediaControllerCallback? = null
  private var playerSpeed: Float = 1.0f
  private var episodeDuration: Long = 0

  // page 635
  private var draggingScrubber: Boolean = false

  private var progressAnimator: ValueAnimator? = null
  private var mediaSession: MediaSessionCompat? = null

  // page 643
  private var mediaPlayer: MediaPlayer? = null

  private var isVideo: Boolean = false
  private var playOnPrepare: Boolean = false

  // chapter 27
  // Episode Player


  // page 616
  companion object {
    fun newInstance(): EpisodePlayerFragment {
      return EpisodePlayerFragment()
    }
  }




  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true

    // update - page 647
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
    } else {
      isVideo = false
    }

    // page 647
    if (!isVideo) {
      initMediaBrowser()
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setupControls()

    // ch 27 p 648
    if (isVideo) {
      initMediaSession()
      initVideoPlayer()
    }
    updateControls()
  }

  override fun onStart() {
    super.onStart()
    if (!isVideo) {
      if (mediaBrowser.isConnected) {
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
          registerMediaController(mediaBrowser.sessionToken)
        }
        updateControlsFromController()
      } else {
        mediaBrowser.connect()
      }
    }
  }

  // page 617
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_episode_player, container, false)
  }



  // page 645, 646
  private fun initVideoPlayer() {

    // 1
    videoSurfaceView.visibility = View.VISIBLE

    // 2
    val surfaceHolder = videoSurfaceView.holder

    // 3
    surfaceHolder.addCallback(object: SurfaceHolder.Callback {

      override fun surfaceCreated(holder: SurfaceHolder) {

        // 4 - page 646
        initMediaPlayer()
        mediaPlayer?.setDisplay(holder)
      }

      override fun surfaceChanged(var1: SurfaceHolder, var2: Int,
                                  var3: Int, var4: Int) {
        // method empty
      }

      override fun surfaceDestroyed(var1: SurfaceHolder) {
        // method empty
      }

    })
  }


  // page 583
  override fun onStop() {
    super.onStop()

    progressAnimator?.cancel()

    val fragmentActivity = activity as FragmentActivity
    if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {

      mediaControllerCallback?.let {
        MediaControllerCompat.getMediaController(fragmentActivity)
            .unregisterCallback(it)

      }
    }

    if (isVideo) {
      mediaPlayer?.setDisplay(null)
    }

    if (!fragmentActivity.isChangingConfigurations) {
      mediaPlayer?.release()
      mediaPlayer = null
    }
  }



  // page 648
  private fun setupVideoUI() {

    episodeDescTextView.visibility = View.INVISIBLE
    headerView.visibility = View.INVISIBLE

    val activity = activity as AppCompatActivity
    activity.supportActionBar?.hide()

    playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
  }



  private fun setupControls() {
    playToggleButton.setOnClickListener {
      togglePlayPause()
    }

    // page 631
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      speedButton.setOnClickListener {
        changeSpeed()
      }
    } else {
      speedButton.visibility = View.INVISIBLE
    }

    // page 633
    forwardButton.setOnClickListener {
      seekBy(30)
    }
    replayButton.setOnClickListener {
      seekBy(-10)
    }


    // page 635

    // 1
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

        // 2
        currentTimeTextView.text = DateUtils.formatElapsedTime((progress / 1000).toLong())
      }

      // page 635
      override fun onStartTrackingTouch(seekBar: SeekBar) {

        // 3
        draggingScrubber = true
      }


      override fun onStopTrackingTouch(seekBar: SeekBar) {

        // 4
        draggingScrubber = false

        // 5
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
          // 6
          controller.transportControls.seekTo(seekBar.progress.toLong())
        } else {
          // 7
          seekBar.progress = 0
        }
      }
    })
  }


  // ch 27
  // page 640
  private fun updateControlsFromController() {

    val fragmentActivity = activity as FragmentActivity
    val controller = MediaControllerCompat.getMediaController(fragmentActivity)

    if (controller != null) {
      val metadata = controller.metadata
      if (metadata != null) {
        handleStateChange(controller.playbackState.state, controller.playbackState.position, playerSpeed)
        updateControlsFromMetadata(controller.metadata)
      }
    }
  }

  // deprecated method OKed by Prof Knautz as this is the exact method used on page 644, as long as it doesn't impact any functionality of the app.
  // page 644
  private fun initMediaPlayer() {
    if (mediaPlayer == null) {

      // 1
      mediaPlayer = MediaPlayer()
      mediaPlayer?.let {

        // 2
        it.setAudioStreamType(AudioManager.STREAM_MUSIC)

        // 3
        it.setDataSource(
          podcastViewModel.activeEpisodeViewData?.mediaUrl)

        // 4
        it.setOnPreparedListener {
          val fragmentActivity = activity as FragmentActivity

          val episodeMediaCallback = PodplayMediaCallback(fragmentActivity, mediaSession!!, it)

          mediaSession!!.setCallback(episodeMediaCallback)

          // 6
          setSurfaceSize()

          // 7
          if (playOnPrepare) {
            togglePlayPause()
          }
        }
        it.prepareAsync()
      }
    } else {
      setSurfaceSize()
    }
  }



  // page 634
  private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
    episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

    endTimeTextView.text = DateUtils.formatElapsedTime((episodeDuration / 1000))

    // page 635
    seekBar.max = episodeDuration.toInt()
  }


  // page 643
  private fun setSurfaceSize() {

    // 1
    val mediaPlayer = mediaPlayer ?: return

    // 2
    val videoWidth = mediaPlayer.videoWidth
    val videoHeight = mediaPlayer.videoHeight

    // 3
    val parent = videoSurfaceView.parent as View
    val containerWidth = parent.width
    val containerHeight = parent.height


    // 4
    val layoutAspectRatio = containerWidth.toFloat() / containerHeight
    val videoAspectRatio = videoWidth.toFloat() / videoHeight

    // 5
    val layoutParams = videoSurfaceView.layoutParams


    // 6
    if (videoAspectRatio > layoutAspectRatio) {
      layoutParams.height = (containerWidth / videoAspectRatio).toInt()
    } else {
      layoutParams.width = (containerHeight * videoAspectRatio).toInt()
    }

    // 7
    videoSurfaceView.layoutParams = layoutParams

  }



  // page 637
  // 1
  private fun animateScrubber(progress: Int, speed: Float) {

    // 2
    val timeRemaining = ((episodeDuration - progress) / speed).toInt()
    if (timeRemaining < 0) {
      return;
    }

    // 4
    progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())


    progressAnimator?.let { animator ->

      // 5
      animator.duration = timeRemaining.toLong()

      // 6
      animator.interpolator = LinearInterpolator()

      // 7
      animator.addUpdateListener {

        if (draggingScrubber) {
          // 8
          animator.cancel()
        } else {
          // 9
          seekBar.progress = animator.animatedValue as Int
        }
      }
      // 10
      animator.start()
    }
  }



  // page 642
  private fun initMediaSession() {

    if (mediaSession == null) {

      // 1
      mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")

      // 2
      mediaSession?.setMediaButtonReceiver(null)
    }
    registerMediaController(mediaSession!!.sessionToken)
  }


  // page 630
  private fun changeSpeed() {

    //1
    playerSpeed += 0.25f

    if (playerSpeed > 2.0f) {
      playerSpeed = 0.75f
    }

    // 2
    val bundle = Bundle()
    bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)

    // 3
    val fragmentActivity = activity as FragmentActivity
    val controller = MediaControllerCompat.getMediaController(fragmentActivity)
    controller.sendCommand(CMD_CHANGESPEED, bundle, null)

    // 4
    speedButton.text = "${playerSpeed}x"
  }



  // page 649
  private fun handleStateChange(state: Int, position: Long, speed: Float) {


    progressAnimator?.let {
      it.cancel()
      progressAnimator = null
    }


    // page 625
    val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
    playToggleButton.isActivated = isPlaying

    val progress = position.toInt()
    seekBar.progress = progress
    speedButton.text = "${playerSpeed}x"



    if (isPlaying) {
      if (isVideo) {
        setupVideoUI()
      }
      // page 639
      animateScrubber(progress, speed)
    }
  }


  // page 621
  private fun updateControls() {

    // 1
    episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title


    // 2
    val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
    val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
    episodeDescTextView.text = descSpan
    episodeDescTextView.movementMethod = ScrollingMovementMethod()


    // 3
    val fragmentActivity = activity as FragmentActivity
    Glide.with(fragmentActivity)
        .load(podcastViewModel.activePodcastViewData?.imageUrl)
        .into(episodeImageView)


    speedButton.text = "${playerSpeed}x"



    mediaPlayer?.let {
      updateControlsFromController()
    }
  }


  // page 605, 606 (update)
  private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {

    // page 587
    val fragmentActivity = activity as FragmentActivity
    val controller = MediaControllerCompat.getMediaController(fragmentActivity)



    val viewData = podcastViewModel.activePodcastViewData ?: return
    val bundle = Bundle()

    // page 606
    bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
    bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feedTitle)
    bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)

    controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
  }


  // page 632
  private fun seekBy(seconds: Int) {
    val fragmentActivity = activity as FragmentActivity
    val controller = MediaControllerCompat.getMediaController(fragmentActivity)
    val newPosition = controller.playbackState.position + seconds * 1000
    controller.transportControls.seekTo(newPosition)
  }

  // page 624
  private fun togglePlayPause() {
    playOnPrepare = true
    val fragmentActivity = activity as FragmentActivity
    val controller = MediaControllerCompat.getMediaController(fragmentActivity)

    if (controller.playbackState != null) {
      if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {

        controller.transportControls.pause()
      } else {

        podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
      }
    } else {
      podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
    }
  }

  // page 582
  private fun initMediaBrowser() {
    val fragmentActivity = activity as FragmentActivity
    mediaBrowser = MediaBrowserCompat(fragmentActivity, ComponentName(fragmentActivity, PodplayMediaService::class.java), MediaBrowserCallBacks(), null)
  }


  // page 580
  private fun registerMediaController(token: MediaSessionCompat.Token) {

    // 2
    val mediaController = MediaControllerCompat(activity, token)
    val fragmentActivity = activity as FragmentActivity

    // 3
    MediaControllerCompat.setMediaController(fragmentActivity, mediaController)

    // 4
    mediaControllerCallback = MediaControllerCallback()
    mediaController.registerCallback(mediaControllerCallback!!)
  }




  // inner classes

  // page 581, 582
  inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {

    //
    override fun onConnected() {
      super.onConnected()

      // 2 register controller
      registerMediaController(mediaBrowser.sessionToken)
      updateControlsFromController()
    }

    // page 581
    override fun onConnectionSuspended() {
      super.onConnectionSuspended()
      println("onConnectionSuspended")
    }

    // page 581
    override fun onConnectionFailed() {
      super.onConnectionFailed()
      println("onConnectionFailed")

    }
  }


  // page 580
  inner class MediaControllerCallback: MediaControllerCompat.Callback() {


    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      super.onMetadataChanged(metadata)
      metadata?.let { updateControlsFromMetadata(it) }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      val state = state ?: return
      handleStateChange(state.state, state.position, state.playbackSpeed)
    }
  }

}
