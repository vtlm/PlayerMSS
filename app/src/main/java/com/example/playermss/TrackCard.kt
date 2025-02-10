package com.example.playermss

import android.R.attr.maxLines
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontVariation.weight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController


@OptIn(UnstableApi::class)
@Composable
fun TrackCard(
    mediaData: MediaData,
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val playingIndex=mediaController?.currentMediaItemIndex
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
                    if (mediaController?.isPlaying != true) {
                        mediaController?.prepare()
                        mediaController?.play()
                    }
                    onClick()
                }),
        shape = RoundedCornerShape(10),
        colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
    ) {
        Row (modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween){
//            if (imageBitmap != null) {
//                Image(
//                    contentScale = ContentScale.FillHeight,
//                    bitmap = imageBitmap,
//                    contentDescription = "some useful description",
//                )
//            }
            Column (Modifier.weight(6f)){//(modifier=Modifier.padding(.dp))
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
                Row{ //(Modifier.background(color = Color.Magenta))

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
                        text = mediaData.mediaData("TALB"),//.dropLast(10),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column (modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
                verticalArrangement = Arrangement.Center){
                Text(modifier = Modifier.align(Alignment.End),
                    text = mediaData.duration())
            }
        }
    }
}
