package com.example.playermss

import android.database.Cursor
import android.provider.MediaStore
import androidx.annotation.OptIn
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

fun duration(durationMs:Long):String{
    val durationS = durationMs?.div(1000)
    val mins = durationS?.div(60)
    val secs = durationS?.rem(60)
    return mins.toString()+":"+secs.toString().padStart(2,'0')
}

@Composable
fun UI_TrackCard(
    cursor: Cursor,
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
){
    val playingIndex=mediaController?.currentMediaItemIndex
//    val imageBitmap= mediaData.pic?.let { imageBitmapFromBytes(it) }

    val listIndex = cursor.position
    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
    val titleColumn =  cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
    val aristColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
    val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

    Card(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
            .padding(2.dp)

            .clickable(
                onClick = {
                    mediaController?.seekTo(listIndex, 0)
                    if (mediaController?.isPlaying != true) {
                        mediaController?.prepare()
                        mediaController?.play()
                    }
                    onClick()
                }),
        shape = RoundedCornerShape(10),
        colors = if(listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
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
                    text = cursor.getString(titleColumn),
//                modifier=Modifier.padding(2.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 16.sp
                )
                Row{ //(Modifier.background(color = Color.Magenta))

                    Text(
                        text =cursor.getString(aristColumn),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "",//cursor.getString(yearColumn),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = cursor.getString(albumColumn),
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
                    text = cursor.getString(durationColumn))
            }
        }
    }

}

@OptIn(UnstableApi::class)
@Composable
fun TrackCardFromCursor(
    cursor: Cursor,
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
 //   UI_TrackCard(cursor,mediaController,modifier,onClick)
    val playingIndex=mediaController?.currentMediaItemIndex
//    val imageBitmap= mediaData.pic?.let { imageBitmapFromBytes(it) }

    val listIndex = cursor.position
    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
    val titleColumn =  cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
    val aristColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
    val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

    val data = cursor.getString(dataColumn)

    Card(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
            .padding(2.dp)

            .clickable(
                onClick = {
                    mediaController?.seekTo(listIndex, 0)
                    if (mediaController?.isPlaying != true) {
                        mediaController?.prepare()
                        mediaController?.play()
                    }
                    onClick()
                }),
        shape = RoundedCornerShape(10),
        colors = if(listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
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
                    text = cursor.getString(titleColumn),
//                modifier=Modifier.padding(2.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 16.sp
                )
                Row{ //(Modifier.background(color = Color.Magenta))

                    Text(
                        text =cursor.getString(aristColumn),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "",//cursor.getString(yearColumn),
//                    modifier = Modifier.padding(2.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = cursor.getString(albumColumn),
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
                    text = duration(cursor.getString(durationColumn).toLong()))
            }
        }
    }

}
