package com.raywenderlich.podplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.raywenderlich.podplay.R

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

import com.raywenderlich.podplay.service.PodplayMediaCallback.PodplayMediaListener
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL


// chapter 26:  Podcast playback

class PodplayMediaService : MediaBrowserServiceCompat(), PodplayMediaListener {

    private lateinit var mediaSession: MediaSessionCompat // page 577

    // page 605
    override fun onStateChanged() {
        displayNotification()
    }


    // page 605
    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    // page 605
    override fun onPausePlaying() {
        stopForeground(false)
    }

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    // page 607
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (parentId.equals(PODPLAY_EMPTY_ROOT_MEDIA_ID)) {
            result.sendResult(null)
        }
    }
    override fun onGetRoot(clientPackageName: String,
                           clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot(
            PODPLAY_EMPTY_ROOT_MEDIA_ID, null)
    }



    // Page 577
    private fun createMediaSession() {

        // 1
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")

        // 2
        setSessionToken(mediaSession.sessionToken)

        val callBack = PodplayMediaCallback(this, mediaSession)
        callBack.listener = this
        mediaSession.setCallback(callBack)  // assign callback
    }

    private fun getPausePlayActions():
            Pair<NotificationCompat.Action, NotificationCompat.Action>  {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white, getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_PAUSE))
        val playAction = NotificationCompat.Action(
            R.drawable.ic_play_arrow_white, getString(R.string.play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_PLAY))
        return Pair(pauseAction, playAction)
    }


    // page 599
    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(this, PodcastActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this@PodplayMediaService, 0, openActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }



    // page 599
    private fun isPlaying(): Boolean {
        if (mediaSession.controller.playbackState != null) {
            return mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING
        } else {
            return false
        }
    }


    // PAGE 599

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(PLAYER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(PLAYER_CHANNEL_ID, "Player", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }


    // page 600 - Chapter 26

    private fun createNotification(mediaDescription: MediaDescriptionCompat, bitmap: Bitmap?): Notification {

        // 2
        val notificationIntent = getNotificationIntent()

        val (pauseAction, playAction) = getPausePlayActions()

        // 4
        val notification = NotificationCompat.Builder(
            this@PodplayMediaService, PLAYER_CHANNEL_ID)


        // 5
        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .addAction(if (isPlaying()) pauseAction else playAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_STOP)))

        // 6 - page 600
        return notification.build()
    }


    companion object {
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id" // page 585

        private const val PLAYER_CHANNEL_ID = "podplay_player_channel" // page 599

        // page 602
        private const val NOTIFICATION_ID = 1
    }

// 602

    private fun displayNotification() {

        // 1
        if (mediaSession.controller.metadata == null) {
            return
        }


        // 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }


        // 3
        val mediaDescription = mediaSession.controller.metadata.description

        GlobalScope.launch {
            val iconUrl = URL(mediaDescription.iconUri.toString())  // 5
            val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())  // 6

            // 7
            val notification = createNotification(mediaDescription, bitmap)


            ContextCompat.startForegroundService(
                this@PodplayMediaService,
                Intent(this@PodplayMediaService, PodplayMediaService::class.java))

            // 9
            startForeground(PodplayMediaService.NOTIFICATION_ID, notification)
        }
    }


}
