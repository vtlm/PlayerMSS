package com.example.playermss.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.playermss.Messages.MediaTrackDataList
import com.example.playermss.data.serializer.MediaTrackDataListSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val USER_PREFERENCES_FILE_NAME = "user_preferences"
private const val DATA_STORE_FILE_NAME = "user_data.pb"
//private const val ARTIST_STORE_FILE_NAME = "user_artist.pb"
//private const val ALBUM_STORE_FILE_NAME = "user_album.pb"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile(USER_PREFERENCES_FILE_NAME) }
        )
    }

    @Singleton
    @Provides
    fun provideProtoDataStore(@ApplicationContext appContext: Context): DataStore<MediaTrackDataList> {
        return DataStoreFactory.create(
            serializer = MediaTrackDataListSerializer,
            produceFile = { appContext.dataStoreFile(DATA_STORE_FILE_NAME) }
        )
    }

//    @Singleton
//    @Provides
//    fun provideProtoDataStoreArtist(@ApplicationContext appContext: Context): DataStore<NameList> {
//        return DataStoreFactory.create(
//            serializer = NamesSerializer,
//            produceFile = { appContext.dataStoreFile(ARTIST_STORE_FILE_NAME) }
//        )
//    }

//    @Singleton
//    @Provides
//    fun provideProtoDataStoreAlbum(@ApplicationContext appContext: Context): DataStore<NameList> {
//        return DataStoreFactory.create(
//            serializer = NamesSerializer,
//            produceFile = { appContext.dataStoreFile(ALBUM_STORE_FILE_NAME) }
//        )
//    }
}
