package com.example.playermss

import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    fun prepareAndPlay(){
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
            if(file.uri.toString().endsWith("mp3")){
                tracksListAdded += MediaData(file.uri, context, onDataReady = ::onFilesMediaDataReady)

                MediaScannerConnection.scanFile(context,arrayOf(file.uri.path,
//                    "/storage/0000-0000/Music/Mane Aura - Space of the Mind (2018)/01. Mane Aura - Firefly.mp3",
//                    "/storage/0000-0000/Music/Mane Aura - Space of the Mind (2018)/02. Mane Aura - Apocalypse on Mars.mp3",
//                    "/storage/0000-0000/Music/Mane Aura - Space of the Mind (2018)/03. Mane Aura - Disco-Droids.mp3",
//                    "/storage/9C33-6BBD/Music/free/Electro/Energoblock/Energoblock - Alienation (Single) - 2009/01 - Energoblock - Alienation.mp3",
//                    "/storage/9C33-6BBD/Music/free/Electro/Energoblock/Energoblock - Alienation (Single) - 2009/02 - Energoblock - Swamp (The Zone).mp3"
                    )
                    ,arrayOf("audio/mp3","*/*"),
                    object: MediaScannerConnectionClient{
                        override fun onScanCompleted(path: String?, uri: Uri?) {
//                            TODO("Not yet implemented")
                            Log.d("DMS","Scan completed: path $path, uri: $uri")
                        }

                        override fun onMediaScannerConnected() {
//                            TODO("Not yet implemented")
                            Log.d("DMS","Scanner connected")
                        }
                    })
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

            prepareAndPlay()
        }
    }

    @OptIn(UnstableApi::class)
    @Composable
 fun asList(modifier: Modifier = Modifier, onClick: () -> Unit = {}){
     val trackListState = remember { tracksListM }
        if(trackListState.isEmpty()){
            Box(modifier=Modifier.fillMaxSize()//.background(color= Color.Yellow)
                     ){
                Text("Use Menu to Add Tracks", modifier = Modifier.align(Alignment.Center))
            }
        }else {
            LazyColumn {
                items(items = trackListState) { item ->
                    TrackCard(mediaData = item, mediaController = mediaController, onClick = onClick)
                }
            }
        }

 }

}




