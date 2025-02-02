package com.example.playermss

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.media3.common.Player
import androidx.media3.session.MediaController

@Composable
fun PlayControls(mediaController: MediaController?, modifier: Modifier) {

    Log.d("DBG_PC", "play ctls called")

    val repeatMode = remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    val maxRepeatModeInd = 2
    val repeatModeId = arrayOf(
        R.drawable.baseline_repeat_24,
        R.drawable.baseline_repeat_one_on_24,
        R.drawable.baseline_repeat_on_24,
    )
    Row {//(modifier = Modifier.weight(1f))
        IconButton(onClick = { mediaController?.seekToPrevious() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                contentDescription = "Skip to Prev"
            )
        }
        IconButton(onClick = {
            mediaController?.prepare()
            mediaController?.play()
        }) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
        }
        IconButton(onClick = { mediaController?.pause() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_pause_24),
                contentDescription = "Pause"
            )
        }
        IconButton(onClick = { mediaController?.seekToNext() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_next_24),
                contentDescription = "Skip to Next"
            )
        }
        IconButton(onClick = {
            repeatMode.intValue += 1
            if (repeatMode.intValue > maxRepeatModeInd) {
                repeatMode.intValue = 0
            }
            mediaController?.repeatMode = repeatMode.intValue
        }) {
            Icon(
                painter = painterResource(id = repeatModeId[repeatMode.intValue]),
                contentDescription = "Repeat Mode"
            )
        }
    }

}
