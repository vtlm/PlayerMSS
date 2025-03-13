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
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream


object MediaTrackDataListSerializer : Serializer<MediaTrackDataList> {
    override val defaultValue: MediaTrackDataList = MediaTrackDataList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MediaTrackDataList {
        try {
            return MediaTrackDataList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: MediaTrackDataList,
        output: OutputStream
    ) = t.writeTo(output)
}



/*
 * Custom app entry point for manual dependency injection
 */
private const val LAYOUT_PREFERENCE_NAME = "layout_preferences"
//private const val APP_DATA_NAME = "app_data"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = LAYOUT_PREFERENCE_NAME
)

private val Context._appDataStore: DataStore<Messages.MediaTrackDataList> by dataStore(
    fileName = "ProtoPreferences.pb",
    serializer = MediaTrackDataListSerializer
)


class PlayerMSSReleaseApplication: Application() {
    lateinit var userPreferencesRepository: com.example.playermss.data.UserPreferencesRepository
    lateinit var appDataStore: DataStore<MediaTrackDataList>

    override fun onCreate() {
        super.onCreate()
        appDataStore =  _appDataStore
        userPreferencesRepository = com.example.playermss.data.UserPreferencesRepository(dataStore)
    }
}