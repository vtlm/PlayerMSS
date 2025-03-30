package com.example.playermss

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel
import kotlinx.coroutines.flow.distinctUntilChanged


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowTrack(trackItem: MediaTrackData, key: Int, selectedKey: Int) {
    Log.d("SII","selected: $selectedKey, current: $key name: ${trackItem.title}")
    val color = if (key == selectedKey) Color.Yellow else Color.White

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(color = color, text = "    ${trackItem.track} - ${trackItem.title}")
        Text(color = color, text = duration(trackItem.duration))
    }
    Log.d("RCT","ReCompose track")
}

private fun LazyListScope.showTracks(tracks: List<MediaTrackData>, mediaViewModel: MediaViewModel){

    tracks.forEach { trackItem ->

        var itemKey = trackItem.getHash()
        if(itemKey == 0){
            itemKey = System.identityHashCode(trackItem)
        }
        item (key = itemKey) {
            Card(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                Log.d("DTTREE", trackItem.title)
                                mediaViewModel.play(trackItem)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
//                                Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
            ) {
                val selected = mediaViewModel.playingItemId.collectAsState()
                ShowTrack(trackItem, itemKey, selected.value)
            }
        }
    }
}

private fun LazyListScope.showAlbums(albums: List<Pair<String, List<MediaTrackData>>>, mediaViewModel: MediaViewModel, albumExpanded: Set<String>){
    albums.forEach {

        val (albumName, albumTracks) = it
        item (key = System.identityHashCode(albumTracks)){//todo: recheck case if albums names same
            Card(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.expandedAlbums.toggle(albumName)
                            },
                            onPress = {
//                                mediaViewModel.toggleAlbumExpanded(albumName)
                            },
                            onLongPress = {
                                Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                shape = RoundedCornerShape(10),
                colors = CardDefaults.elevatedCardColors()
            ) {
                val year =
                if(albumTracks.isNotEmpty()){
                    albumTracks[0].year
                }else{
                    ""
                }
                Text("  $year - $albumName")
            }
        }

        if(albumExpanded.contains(albumName)) {
            showTracks(albumTracks, mediaViewModel)
        }
    }
}

private fun LazyListScope.showArtist(artistAsPair: Pair<String, List<Pair<String, List<MediaTrackData>>>>, mediaViewModel: MediaViewModel, artistExpanded: Set<String>, albumExpanded: Set<String>){

    val (artistName, artistAlbums) = artistAsPair

    item (key = System.identityHashCode(artistAlbums)) {
        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                .padding(2.dp)
                .clickable(
                    onClick = {
                        mediaViewModel.expandedArtists.toggle(artistName)
                    }),
            shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
        ) {
            Text("Artist: ${artistName}. albums: ${artistAlbums.count()}")
        }
    }

    if(artistExpanded.contains(artistName)) {
        showAlbums(artistAlbums, mediaViewModel, albumExpanded)
    }
}

@Composable
fun ShowQueryResults(
    results: List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>,
    artistExpanded: Set<String>,
    albumExpanded: Set<String>,
    scrollPos: Int?,
    mediaViewModel: MediaViewModel
){
    Log.d("DBGL","Recomp with scrollposL $scrollPos")
    val listState = rememberLazyListState(scrollPos ?: 0)
//    listState.firstVisibleItemIndex = scrollPos

    LaunchedEffect(scrollPos) {
//        delay(300)
        val offs = listState.firstVisibleItemScrollOffset
        Log.d("DBGL","offs $offs")
        listState.scrollToItem(scrollPos ?: 0, offs)
    }

//    LaunchedEffect(listState) {
//        snapshotFlow { listState}
//            .distinctUntilChanged()
//            .collect {
//                Log.d("DBGL", "State updated")
//                mediaViewModel.laztListState = listState
//            }
//    }


    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .distinctUntilChanged()
            .collect {
//                mediaViewModel.setLazyListTotalItemsCount(it)// listState.firstVisibleItemIndex)
//                mediaViewModel.visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                mediaViewModel.lazyListState = listState

            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
            .distinctUntilChanged()
            .collect {
//                mediaViewModel.setLazyListVisibleItemsCount(it)// listState.firstVisibleItemIndex)
//                mediaViewModel.visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                mediaViewModel.lazyListState = listState

            }
    }

    LaunchedEffect(listState) {

//        listState.scrollToItem(listState.firstVisibleItemIndex)

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
                Log.d("DBGL","${it} ${listState.layoutInfo.totalItemsCount} " +
                        "${listState.layoutInfo.visibleItemsInfo.size} ${listState.layoutInfo.viewportStartOffset}")
//                MyAnalyticsService.sendScrolledPastFirstItemEvent()
                mediaViewModel.setUserScrollPos(it)// listState.firstVisibleItemIndex)
//                mediaViewModel.visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                mediaViewModel.lazyListState = listState

            }


    }

    DisposableEffect(results) {
        onDispose {
//            val k = listState.firstVisibleItemIndex
//            mediaViewModel.setRemainTime(true)//!mediaViewModel.isRemainTime.value)
        }
    }


    if(results.isNotEmpty()) {
        LazyColumn(state = listState) {
            results.forEach {
                showArtist(it, mediaViewModel, artistExpanded, albumExpanded)
            }
        }
    }else{
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ){
            Column( horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search in Library",
                    modifier = Modifier.scale(2f).padding(24.dp)
                )
                Text(
                    text = "Use query to search in Library",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

