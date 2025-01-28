package com.example.playermss

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

//todo: move adapter to level up to TrackList
//todo dataset to trackdata
class TrackList(private val context: Context, private val mediaController: MediaController? = null){

    private var tracksListAdded=arrayListOf<MediaData>()
//    private var mediaSources = arrayListOf<MediaItem>()
    private var tracksListM=mutableStateListOf<MediaData>()

    fun clear(){
        tracksListM.clear()
//        mediaSources.clear()
    }

    fun play(){
//        mediaSources.clear()
//        for(t in tracksList){
//            mediaSources += MediaItem.Builder().setUri(t.uri).build()
//        }
//        player.setMediaItems(mediaSources)
        mediaController?.prepare()
        mediaController?.play()
    }

    @OptIn(UnstableApi::class)
    fun updateColumnIndices(){
        tracksListM.forEachIndexed { index, mediaData -> mediaData.listIndex=index }
    }

    @OptIn(UnstableApi::class)
    fun addFromFiles(files:Array<DocumentFile>){
        tracksListAdded.clear()

        for(file in files){
            if(file.uri.toString().contains("mp3")){
                tracksListAdded += MediaData(file.uri, context, onDataReady = ::onFilesMediaDataReady)
            }
        }

        for(t in tracksListAdded){
            t.requestMediaData()
        }

    }

    @OptIn(UnstableApi::class)
    fun onFilesMediaDataReady(){
        if(!(tracksListAdded.find {it.requestDone == 0} != null)){
            tracksListM.addAll(tracksListAdded)
            updateColumnIndices()
            for(t in tracksListAdded){
                mediaController?.addMediaItem(MediaItem.Builder().setUri(t.uri).build())
            }
            Log.d("TLMS","size=$tracksListM.size()")

            play()
        }

    }

    @OptIn(UnstableApi::class)
    @Composable
 fun asList(modifier: Modifier= Modifier){
     val trackListState = remember {tracksListM }
     LazyColumn {
         items(items=trackListState){
             item->
             trackCard(mediaData = item, mediaController = mediaController)
         }
     }

 }

}




