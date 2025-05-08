package com.example.playermss

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.playermss.data.MediaViewModel

@Composable
fun PlayControls(
    isPlaying: Boolean,
    playerRepeatMode: Int,
    mediaViewModel: MediaViewModel,
    modifier: Modifier
) {

    //Log.d("DBG_PC", "play ctls called")

    val repeatModeId = arrayOf(
        R.drawable.baseline_repeat_24,
        R.drawable.baseline_repeat_one_on_24,
        R.drawable.baseline_repeat_on_24,
    )
    Row {//(modifier = Modifier.weight(1f))
        IconButton(onClick = { mediaViewModel.incPlayerRepeatMode() }) {
            Icon(
                painter = painterResource(id = repeatModeId[playerRepeatMode]),
                contentDescription = "Repeat Mode"
            )
        }
        IconButton(onClick = { mediaViewModel.seekToPrevious() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                contentDescription = "Skip to Prev"
            )
        }
        if (isPlaying) {
            IconButton(onClick = {
                mediaViewModel.pause()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_pause_24),
                    contentDescription = "Pause"
                )
            }
        } else {
            IconButton(onClick = {
                mediaViewModel.play()
            }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
            }
        }
        IconButton(onClick = { mediaViewModel.seekToNext() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_next_24),
                contentDescription = "Skip to Next"
            )
        }
    }

}
