package com.example.playermss

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

@Composable
fun ButtonAction(caption: String, onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(caption)
    }
}



@OptIn(UnstableApi::class)
@Composable
fun TrackInfo(mediaMetadata: MediaMetadata?, modifier: Modifier =Modifier) {

    if (mediaMetadata == null) {
        Text("Select a track to play",modifier)
    } else {
        var songInfo = "";
        mediaMetadata.artist?.let {
            Log.d("DBGC", it.toString())
            songInfo += it.toString()
        }
        mediaMetadata.albumTitle?.let {
            Log.d("DBG", it.toString())
            songInfo += "-"
            songInfo += it.toString()
        }
        mediaMetadata.title?.let {
            Log.d("DBG", it.toString())
            songInfo += "-"
            songInfo += it.toString()
        }
        Text(songInfo,modifier)
    }
}
