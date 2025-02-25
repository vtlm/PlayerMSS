package com.example.playermss

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel

private fun LazyListScope.showTracks(tracks: List<MediaTrackData>, mediaViewModel: MediaViewModel){
    tracks.forEach { track ->

        item {
            Card(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)
                    .clickable(
                        onClick = {
//                            mediaViewModel.toggleAlbumExpanded(it.key)
                        })
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.play(track)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
//                                Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
            ) {
                Text("        ${track.title}")
            }
        }
    }
}

private fun LazyListScope.showAlbums(albums: Map<String, List<MediaTrackData>>, mediaViewModel: MediaViewModel, albumExpanded: Set<String>){
    albums.forEach(){
        val albumName = it.key
        item {
            Card(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)
                    .clickable(
                        onClick = {
//                            mediaViewModel.toggleAlbumExpanded(it.key)
                        })
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
                                Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
            ) {
                Text("    ${it.key}")
            }
        }

        if(albumExpanded.contains(it.key)) {
            showTracks(it.value, mediaViewModel)
        }
    }
}

private fun LazyListScope.showArtist(artist: Map. Entry<String, Map<String, List<MediaTrackData>>>, mediaViewModel: MediaViewModel, artistExpanded: Set<String>, albumExpanded: Set<String>){

    item {
        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                .padding(2.dp)
                .clickable(
                    onClick = {
                        mediaViewModel.toggleArtistExpanded(artist.key)
//                            mediaController?.seekTo(mediaData.listIndex, 0)
//                            if (mediaController?.isPlaying != true) {
//                                mediaController?.prepare()
//                                mediaController?.play()
//                            }
                    }),
            shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
        ) {
            Text("Artist: ${artist.key}")
        }
    }

    if(artistExpanded.contains(artist.key)) {
        showAlbums(artist.value,mediaViewModel,albumExpanded)
    }
}


@Composable
fun ShowSearchResults(mediaViewModel: MediaViewModel){

    val results = mediaViewModel.searchResults.collectAsState()
    val artistExpanded = mediaViewModel.expandedArtists.collectAsState()
    val albumExpanded = mediaViewModel.expandedAlbums.collectAsState()

    LazyColumn {
        results.value.forEach(){
            showArtist(it,mediaViewModel,artistExpanded.value,albumExpanded.value)
        }
    }
}

