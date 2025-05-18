package com.example.playermss

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/*
@Preview
@Composable
fun TestConstraint(){
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (c1, c2) = createRefs()
        Box(modifier = Modifier.constrainAs(c1){
            end.linkTo(parent.end)
        }
            .padding(start = 4.dp, end = 2.dp)
//            .wrapContentSize()
            .background(color = Color.Cyan)
        ){
            Text(color = Color.Black, text = "3333333")
        }
        Box(modifier = Modifier.constrainAs(c2){
            start.linkTo(parent.start)
            end.linkTo(c1.start)
        }
//            .fillMaxWidth()
            .background(color = Color.Magenta)
//            .padding(start = 4.dp, end = 4.dp)
//            .wrapContentSize()
//, contentAlignment = Alignment.TopStart

        )

        {
            Text( color = Color.Blue,
                modifier = Modifier.align(Alignment.TopStart),
//                    overflow = TextOverflow.Ellipsis,
                text = "titleddddddd")
        }

    }

}

@Preview
@Composable
fun TestShowTrackTitle(){
    ShowTrackTitle(1,"Title Title Title Title Title Title Title Title Title Title Title Title Title Title Title Title Title ",400000,Color.White)
}

@Composable
fun ShowTrackTitle(track: Int?, title: String, duration: Int?, color: Color){
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(5f)) {
            Text(modifier = Modifier.align(Alignment.Start), color = color, text = "$track - $title")
        }
        Column(Modifier.fillMaxWidth().weight(1f)) {
            Text(modifier = Modifier.align(Alignment.End), color = color, text = duration(duration))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowTrack(trackItem: MediaTrackData, key: Int, selectedKey: Int) {
    //Log.d("SII","selected: $selectedKey, current: $key name: ${trackItem.title}")
    val color = if (key == selectedKey) Color.Yellow else MaterialTheme.colorScheme.onSecondaryContainer

    ShowTrackTitle(trackItem.track, trackItem.title, trackItem.duration, color)
}
*/
private fun LazyListScope.showTracks(tracks: List<MediaTrackData>, mediaViewModel: MediaViewModel){

    tracks.forEach { trackItem ->

        var itemKey = trackItem.getHash()
        if(itemKey == 0){
            itemKey = System.identityHashCode(trackItem)
        }
        item (key = itemKey) {
            Surface(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                //Log.d("DTTREE", trackItem.title)
                                mediaViewModel.play(trackItem)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
//                                //Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.secondaryContainer,
//                shape = RoundedCornerShape(10),
            ) {
                val selected = mediaViewModel.playingItemId.collectAsState().value
                ShowTrack(trackItem, itemKey, selected)
            }
        }
    }
}

private fun LazyListScope.showAlbums(albums: List<Pair<Pair<String, String>, List<MediaTrackData>>>?, mediaViewModel: MediaViewModel, albumExpanded: Set<String>){
    albums?.forEach {

        val (albumPair, albumTracks) = it
        val (albumPath, albumName) = albumPair

        item (key = System.identityHashCode(it)){//todo: recheck case if albums names same
            Surface (
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.expandedAlbums.toggle(albumPath)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
                                //Log.d("LP","Album long press: $albumKey")
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.primaryContainer,
//                shape = RoundedCornerShape(10),
            ) {
                val year =
                    if(albumTracks.isNotEmpty()){
                        albumTracks[0].year
                    }else{
                        ""
                    }
                val albumName =
                    if(albumTracks.isNotEmpty()){
                        albumTracks[0].album
                    }else{
                        ""
                    }

                var descr = ""
                if (albumPath != ""){
                    val s =albumPath.split('/')
                    if(s.size > 1) {
                        descr = s[s.size - 2]
                    }
                }

                var namesMatch = descr.endsWith(albumName,true)

                if(!namesMatch){
                    descr = descr.replace(year.toString(),"")
                    descr = descr.replace(albumName,"")
                    descr = descr.replace(" - ","")
                }

                if(albumName.contains(descr)){
                    namesMatch = true
                }

                Row {
                    Box(
                        Modifier.fillMaxHeight().padding(start = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            text = "$year"
                        )
                    }
                    Column (Modifier.fillMaxWidth(),
//                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            text = albumName
                        )
                        if(!namesMatch) {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                text = descr
                            )
                        }
                        }
                }
            }
        }

        if(albumExpanded.contains(albumPath)) {
            showTracks(albumTracks, mediaViewModel)
        }
    }
}

@Composable
fun ShowQueryResultsAlbumTrack(
    results:List<Pair<Pair<String, String>, List<MediaTrackData>>>?,
    albumExpanded: Set<String>,
    scrollPos: Int?,
    mediaViewModel: MediaViewModel
){
    //Log.d("DBGL","Recomp with scrollposL $scrollPos")
    val listState = rememberLazyListState(scrollPos ?: 0)
//    listState.firstVisibleItemIndex = scrollPos

    LaunchedEffect(scrollPos) {
        val offs = listState.firstVisibleItemScrollOffset
        //Log.d("DBGL","offs $offs")
        listState.scrollToItem(scrollPos ?: 0, offs)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .distinctUntilChanged()
            .collect {
                mediaViewModel.lazyListState = listState
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
            .distinctUntilChanged()
            .collect {
                mediaViewModel.lazyListState = listState
            }
    }

    LaunchedEffect(listState) {

        snapshotFlow { listState.firstVisibleItemIndex }
//            .map { index -> index > 0 }
            .distinctUntilChanged()
//            .timeout(500.milliseconds).catch { exception ->
//        if (exception is TimeoutCancellationException) {
//            // Catch the TimeoutCancellationException emitted above.
//            // Emit desired item on timeout.
//            emit(listState.firstVisibleItemIndex)
//        } else {
//            // Throw other exceptions.
//            throw exception
//        }}

//            .filter { it == true }
            .collect {
                Timber.d("${it} ${listState.layoutInfo.totalItemsCount} " +
                        "${listState.layoutInfo.visibleItemsInfo.size} ${listState.layoutInfo.viewportStartOffset}")
                mediaViewModel.setUserScrollPos(it)// listState.firstVisibleItemIndex)
                mediaViewModel.lazyListState = listState

            }


    }

    DisposableEffect(results) {
        onDispose {
//            val k = listState.firstVisibleItemIndex
//            mediaViewModel.setRemainTime(true)//!mediaViewModel.isRemainTime.value)
        }
    }

    LazyColumn(state = listState) {
            showAlbums(results, mediaViewModel, albumExpanded)
    }
}

