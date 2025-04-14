package com.example.playermss

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi

@Composable
fun SleevePicture(mediaMetadata: MediaMetadata?){
    val bitmap = mediaMetadata?.artworkData?.let {
        imageBitmapFromBytes(it)
    }

    if(bitmap != null){
        DrawPicture(bitmap)
    }else{
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {

            val lines: MutableList<String> = mutableListOf()
            mediaMetadata?.title?.let {
                lines += it.toString()
            }
            mediaMetadata?.artist?.let {
                lines += it.toString()
            }
            mediaMetadata?.albumTitle?.let {
                lines += it.toString()
            }
            mediaMetadata?.recordingYear?.let {
                lines += it.toString()
            }

            SparseLines(lines)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun DrawPicture(imageBitmap: ImageBitmap?){
    if (imageBitmap != null) {
        Box(Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.fillMaxSize()
                    .padding(8.dp),
                bitmap = imageBitmap,
                contentScale = ContentScale.FillHeight,
                contentDescription = "Sleeve picture",
            )
        }
    }
}