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

private fun LazyListScope.showTracks(tracks: List<MediaTrackData>,
                                     isVariousArtists: Boolean,
                                     mediaViewModel: MediaViewModel){

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
                    .padding(2.dp)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.play(trackItem)
                            },
                            onPress = {
                            },
                            onLongPress = {
//                                //Log.d("LP","Album long press: $albumName")
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                val selected = mediaViewModel.playingItemId.collectAsState().value
                ShowTrack(trackItem, itemKey, selected, isVariousArtists)
            }
        }
    }
}

private fun LazyListScope.showAlbums(albums: List<Pair<Pair<String, String>, List<MediaTrackData>>>?, mediaViewModel: MediaViewModel, albumExpanded: Set<String>){
    albums?.forEach {

        val (albumPair, albumTracks) = it
        val (albumPath, albumName) = albumPair

        var isVariousArtists = false

        if(albumTracks.isNotEmpty()){
            val firstArtist = albumTracks.first().artist
            isVariousArtists = albumTracks.find { it.artist != firstArtist } != null
        }

        item (key = System.identityHashCode(it)){//todo: recheck case if albums names same
            Surface (
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
                    .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                    .pointerInput(Unit){
                        detectTapGestures (
                            onTap = {
                                mediaViewModel.expandedAlbums.toggle(albumPath)
                            },
                            onPress = {
                            },
                            onLongPress = {
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                val year =
                    if(albumTracks.isNotEmpty()){
                        albumTracks[0].year
                    }else{
                        ""
                    }
                val aristName = if (isVariousArtists){
                    "Various Artists"
                }else {
                    if (albumTracks.isNotEmpty()) {
                        albumTracks[0].artist
                    } else {
                        ""
                    }
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
                            text = aristName
                        )
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
            showTracks(albumTracks, isVariousArtists, mediaViewModel)
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
    val listState = rememberLazyListState(scrollPos ?: 0)

    LaunchedEffect(scrollPos) {
        val offs = listState.firstVisibleItemScrollOffset
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
            .distinctUntilChanged()
            .collect {
                Timber.d("${it} ${listState.layoutInfo.totalItemsCount} " +
                        "${listState.layoutInfo.visibleItemsInfo.size} ${listState.layoutInfo.viewportStartOffset}")
                mediaViewModel.setUserScrollPos(it)// listState.firstVisibleItemIndex)
                mediaViewModel.lazyListState = listState
            }
    }

    LazyColumn(state = listState) {
            showAlbums(results, mediaViewModel, albumExpanded)
    }
}

