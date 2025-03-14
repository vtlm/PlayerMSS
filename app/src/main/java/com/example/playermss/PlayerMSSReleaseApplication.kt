package com.example.playermss

import android.app.Application
import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.playermss.Messages.MediaTrackDataList
import com.example.playermss.data.UserDataRepository
import com.example.playermss.data.serializer.MediaTrackDataListSerializer
import com.google.protobuf.InvalidProtocolBufferException
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
}