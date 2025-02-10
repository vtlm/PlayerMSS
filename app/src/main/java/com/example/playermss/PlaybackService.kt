package com.example.playermss

import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService


class PlaybackService : MediaSessionService() {
    var player: Player? = null
    private var mediaSession: MediaSession? = null

    // Create your player and media session in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
         player = ExoPlayer.Builder(this).build()
         mediaSession = MediaSession.Builder(this, player!!).build()
         Log.d("OC","Playback service created")

        val mainHandler = Handler(mainLooper)
        mainHandler.post { // Do your stuff here related to UI, e.g. show toast
            Toast.makeText(applicationContext, "Playback service created", Toast.LENGTH_LONG).show()
        }
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {

        val mainHandler: Handler = Handler(mainLooper)

        mainHandler.post(Runnable { // Do your stuff here related to UI, e.g. show toast
            Toast.makeText(applicationContext, "I'm a toast!, removed", Toast.LENGTH_LONG).show()
        })

        val player = mediaSession?.player!!
        if (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
            ) {
            // Stop the service if not playing, continue playing in the background
            // otherwise.
            stopSelf()
        }else{
            player.pause()
            stopSelf()
        }

        Log.d("MSS","task removed")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
        Log.d("MSS","destroyed")

    }
}