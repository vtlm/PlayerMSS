package com.example.playermss

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController

fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(encodedImageData, 0, encodedImageData.size).asImageBitmap()
}

@Composable
fun SleevePicture(mediaController: MediaController?){

    val cMediaMetadata = remember { mutableStateOf<MediaMetadata?>(null) }

    LaunchedEffect(key1 = mediaController) {
        mediaController?.addListener(
            object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)
                        cMediaMetadata.value = mediaMetadata

                }

            }
        )
    }//LaunchedEffect

    val imageBitmap= cMediaMetadata.value?.artworkData?.let { imageBitmapFromBytes(it) }

    if (imageBitmap != null) {
        Image(
            modifier = Modifier.fillMaxSize(.95f).padding(2.dp),
//            contentScale = ContentScale.,
            bitmap = imageBitmap,
            contentDescription = "some useful description",
        )
    }


}