package com.example.playermss

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

fun duration(durationMs:Int?):String{
    val durationS = durationMs?.div(1000)
    val mins = durationS?.div(60)
    val secs = durationS?.rem(60)
    return mins.toString()+":"+secs.toString().padStart(2,'0')
}

fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(encodedImageData, 0, encodedImageData.size).asImageBitmap()
}


@Composable
fun ButtonAction(caption: String, onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(caption)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable fun ShowProgressOrContent(progressTitle: String, content: @Composable () -> Unit){

    LaunchedEffect(progressTitle) {
        while(progressTitle != ""){
            Log.d("CRT", "from ShowProgress")
            delay(1000)
        }
    }

    if(progressTitle != ""){
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Column (horizontalAlignment = Alignment.CenterHorizontally){
                Text(
                    text = progressTitle,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.8f).padding(8.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }else {
        content()
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
        mediaMetadata.recordingYear?.let {
            Log.d("DBG", it.toString())
            songInfo += "-"
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
