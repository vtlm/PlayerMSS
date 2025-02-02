package com.example.playermss

import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

fun imageBitmapFromBytes(encodedImageData: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(encodedImageData, 0, encodedImageData.size).asImageBitmap()
}
@OptIn(UnstableApi::class)
@Composable
fun TrackCard(
    mediaData: MediaData,
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
    val imageBitmap= mediaData.pic?.let { imageBitmapFromBytes(it) }
    Card(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
            .padding(2.dp)
            .clickable(
                onClick = {
                    mediaController?.seekTo(mediaData.listIndex, 0)
                    if(mediaController?.isPlaying != true) {
                        mediaController?.prepare()
                        mediaController?.play()
                    }
                }),
        shape = RoundedCornerShape(10),
    ) {
        Row (modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween){
            if (imageBitmap != null) {
                Image(
                    contentScale = ContentScale.FillHeight,
                    bitmap = imageBitmap,
                    contentDescription = "some useful description",
                )
            }
            Column {//(modifier=Modifier.padding(.dp))
//        {
//            Image(painter = painterResource(id = affirmation.imageResourceId),
//                contentDescription = stringResource(id = affirmation.stringResourceId),
//                modifier= Modifier
//                    .fillMaxWidth()
//                    .height(194.dp),
//                contentScale = ContentScale.Crop
//            )
                Text(
                    text = mediaData.mediaData("TIT2"),
//                modifier=Modifier.padding(2.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 16.sp
                )
                Row {
                    Text(
                        text = mediaData.mediaData("TPE1", " - "),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = mediaData.mediaData("TYER", " - "),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = mediaData.mediaData("TALB"),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                }
            }
            Column (modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center){
                Text(modifier = Modifier.align(Alignment.End),
                    text = mediaData.duration())
            }
        }
    }
}
