package com.example.playermss.data

import android.net.Uri
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
import javax.inject.Inject
import androidx.core.net.toUri
import com.example.playermss.StringSet
import kotlinx.coroutines.flow.MutableStateFlow

class UserDataRepository @Inject constructor(private val dataStore: DataStore<MediaTrackDataList>,
//                                             private val expArtistDataStore: DataStore<StringSet.NameList>,
//                                             private val expAlbumDataStore: DataStore<StringSet.NameList>,
    ) {


    fun asStateFlow(launchScope:CoroutineScope): StateFlow<List<MediaTrackData>> {
        return dataStore.data.map {
            toMediaTrackDataList(it)
        }.stateIn(scope = launchScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = runBlocking {
                toMediaTrackDataList(dataStore.data.first())
            }
        )
    }

    //todo asSortedTreeStateFlow ? may be not

    private fun toMediaTrackDataList(items: Messages.MediaTrackDataList):List<MediaTrackData>{
        val trackList = mutableListOf<MediaTrackData>()
        for(i in 0..<items.tracksCount){
            val item = items.getTracks(i)
            trackList += MediaTrackData(
                item.artist,
                item.album,
                item.title,
                item.year,
                item.track,
                item.duration,
                item.uri.toUri()
            )
        }
        return trackList
    }

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

//    fun toSet(list: StringSet.NameList):Set<String>{
//        val outSet = mutableSetOf<String>()
//        for(i in 0..<list.nameCount){
//            outSet += list.getName(i)
//        }
//        return outSet
//    }
//
//    fun artistAsStateFlow(launchScope:CoroutineScope): StateFlow<Set<String>> {
//        return expArtistDataStore.data.map {
//            toSet(it)
//        }.stateIn(scope = launchScope,
//            started = SharingStarted.WhileSubscribed(5_000),
//            initialValue = runBlocking {
//                toSet(expArtistDataStore.data.first())
//            }
//        )
//    }
//
//    suspend fun saveExpArtist(set: Set<String>){
//        expArtistDataStore.updateData { names ->
//
////            val items = mutableListOf<String>()
////
////            set.forEach {
////                items += it
////            }
//
//            names.toBuilder()
//                .clear()
//                .setDirectLogic(true)
//                .addAllName(set.toList())
//                .build()
//        }
//    }


}