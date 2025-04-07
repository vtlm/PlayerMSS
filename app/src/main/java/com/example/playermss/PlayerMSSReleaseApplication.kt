package com.example.playermss

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.data.MediaViewModel
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.HiltAndroidApp
import java.io.InputStream
import java.io.OutputStream


/*
 * Custom app entry point for manual dependency injection
 */
//private const val LAYOUT_PREFERENCE_NAME = "layout_preferences"
//private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
//    name = LAYOUT_PREFERENCE_NAME
//)
//
//private val Context.appDataStore: DataStore<Messages.MediaTrackDataList> by dataStore(
//    fileName = "ProtoPreferences.pb",
//    serializer = MediaTrackDataListSerializer
//)

@HiltAndroidApp
class PlayerMSSReleaseApplication: Application() {
//    lateinit var userPreferencesRepository: com.example.playermss.data.UserPreferencesRepository
//    lateinit var userDataRepository: com.example.playermss.data.UserDataRepository
//
//    override fun onCreate() {
//        super.onCreate()
//        userPreferencesRepository = com.example.playermss.data.UserPreferencesRepository(dataStore)
//        userDataRepository = com.example.playermss.data.UserDataRepository(appDataStore)
//    }


    override fun onCreate() {
    super.onCreate()

    val sessionToken =
        SessionToken(
            applicationContext,
            ComponentName(applicationContext, PlaybackService::class.java)
        )

    val controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()

    controllerFuture.addListener({
        // MediaController is available here with controllerFuture.get()
        mediaController = controllerFuture.get()

//        mediaController.removeListener(listener)
//        mediaController.addListener(listener)


    }, MoreExecutors.directExecutor())
}


    companion object {
        var prevListener: Player.Listener? = null

        lateinit var appContext: Context
        lateinit var mediaController: MediaController
//        var lastListener: Player.Listener
//        var lastMediaController: MediaController? = null

        fun isMediaControllerInitialized(): Boolean{
            return ::mediaController.isInitialized
        }

        fun setPlayerListener(listener: Player.Listener){
            prevListener?.let{
                mediaController.removeListener(prevListener!!)
            }
            mediaController.addListener(listener)
            prevListener = listener
        }

    }

}