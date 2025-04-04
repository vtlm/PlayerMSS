package com.example.playermss

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService


class PlaybackService : MediaSessionService() {
    private lateinit var player: Player
    private lateinit var mediaSession: MediaSession

    // Create your player and media session in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
         player = ExoPlayer.Builder(this).build()
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes,true)

         mediaSession = MediaSession.Builder(this, player)
             .setSessionActivity(getSessionActivityIntent())
             .build()
         Log.d("OC","Playback service created")


//        val mainHandler = Handler(mainLooper)
//        mainHandler.post { // Do your stuff here related to UI, e.g. show toast
//            Toast.makeText(applicationContext, "Playback service created", Toast.LENGTH_LONG).show()
//        }
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {

//        val mainHandler: Handler = Handler(mainLooper)
//
//        mainHandler.post(Runnable { // Do your stuff here related to UI, e.g. show toast
//            Toast.makeText(applicationContext, "I'm a toast!, removed", Toast.LENGTH_LONG).show()
//        })

        val player = mediaSession.player
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
        Log.d("MSS","destroyed")

    }
}

@OptIn(UnstableApi::class)
private fun Context.getSessionActivityIntent(): PendingIntent {
    return PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
