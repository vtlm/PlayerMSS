package com.example.playermss.data

import androidx.datastore.core.DataStore
import com.example.playermss.Messages
import com.example.playermss.Messages.MediaTrackDataList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

class UserDataRepository(private val dataStore: DataStore<MediaTrackDataList>) {

    fun asStateFlow(launchScope:CoroutineScope): StateFlow<Messages.MediaTrackDataList> {
        return dataStore.data.map {
            it
        }.stateIn(scope = launchScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = runBlocking {
                dataStore.data.first()
            }
        )
    }

    //todo asSortedTreeStateFlow

    suspend fun saveTrackList(list: List<MediaTrackData>){
        dataStore.updateData { mediaTrackDataList ->

            val items = mutableListOf<Messages.MediaTrackData>()

            list.forEach {
                val item = Messages.MediaTrackData.newBuilder()
                item.artist = it.artist
                item.album = it.album
                item.title = it.title
                it.track?.let { track -> item.track = track }
                it.year?.let { year -> item.year = year }
                it.duration?.let { duration -> item.duration = duration }
                it.uri?.let { uri -> item.uri = uri.toString() }
                items += item.build()
            }

            mediaTrackDataList.toBuilder()
                .clearTracks()
                .addAllTracks(items)
                .build()
        }
    }

}