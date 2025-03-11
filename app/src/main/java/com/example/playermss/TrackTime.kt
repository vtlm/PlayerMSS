package com.example.playermss

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.playermss.data.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow


@Composable
fun TrackTime(mediaViewModel: MediaViewModel) {
 //   Column {
//        TstBool(mediaViewModel.isRemainTime.collectAsState().value, mediaViewModel::setRemainTime)
        TrackTime(mediaViewModel.mediaController, mediaViewModel.isRemainTime.collectAsState().value, mediaViewModel::setRemainTime)
//    }
}

@Composable
fun TstBool(remaining: Boolean, setRemainTime:(Boolean)->Unit){
    Text(modifier = Modifier.clickable(onClick = { setRemainTime(!remaining) }),
        text = "click $remaining")
}

@Composable
fun TrackTime(mediaController: MediaController?, remaining: Boolean, setRemainTime:(Boolean)->Unit) {
//fun TrackTime(mediaViewModel: MediaViewModel) {

//    val mediaController = mediaViewModel.mediaController
    var currentTime by remember { mutableLongStateOf(0L) }
    var fullTime by remember { mutableLongStateOf(0L) }
    val cMediaMetadata = remember { mutableStateOf<MediaMetadata?>(null) }
    var userInteraction by remember {  mutableIntStateOf(0) }

    val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Log.d("DMG_CR", "CR mediaData changed")
            cMediaMetadata.value = mediaMetadata
            if(mediaMetadata.title == null){
                fullTime = 0
                currentTime = 0
            }
        }
    }

    LaunchedEffect(mediaController) {
        mediaController?.addListener(playerListener)

        while (true) {
//            Log.d("CRT", "Hello World from LE coroutine $currentTime, userInteraction $userInteraction")
            if ((userInteraction == 0) && mediaController?.isPlaying == true) {
                mediaController.contentDuration.let { fullTime = it }
                mediaController.currentPosition.let { currentTime = it }
            }
            delay(1000)
        }
    }//LaunchedEffect(mediaController)

    DisposableEffect(mediaController) {
        onDispose {
            mediaController?.removeListener(playerListener)
            Log.d("DDE","Remove listener")
        }
    }

//    val remaining = mediaViewModel.isRemainTime.collectAsState().value//by remember { mutableIntStateOf(0) }
    val calcTime = if (remaining) fullTime - currentTime else currentTime
    val time = "%1\$tM:%1\$tS".format(calcTime)
    val showTime = if (remaining) "-$time" else time
//    Log.d("TIME", "$currentTime")

    Column {
        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                .padding(horizontal = 2.dp, vertical = 0.dp)
                .clickable(onClick = { setRemainTime(!remaining) }),
            shape = RoundedCornerShape(10),

            ) {
            Row(modifier = Modifier.align(alignment = Alignment.End)) {
//                    TrackInfo(cMediaMetadata.value)
                Text(
                    showTime,
                    Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.End,
                    fontSize = 24.sp
                )
            }
        }
        Slider(
            value = currentTime / 1000F,
            onValueChange = { currentTime = (it * 1000).toLong()
                userInteraction = 1},
            onValueChangeFinished = { mediaController?.seekTo(currentTime)
                userInteraction = 0 },
            valueRange = 0f..fullTime / 1000F
        )
    }
}
