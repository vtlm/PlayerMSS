package com.example.playermss

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.SearchHelper

class PlayTracksManager(val mediaController: MediaController?) {

    private var currentMediaTrackData: MediaTrackData? = null
    private var prevMediaTrackData: MediaTrackData? = null
    private var nextMediaTrackData: MediaTrackData? = null

    lateinit var searchHelper: SearchHelper

    fun setTrack(mediaTrackData: MediaTrackData){


        Log.d("DTP", mediaTrackData.title)
        mediaController?.clearMediaItems()

        var offset = 0

        currentMediaTrackData = mediaTrackData
//        _playingItemId.value = System.identityHashCode(mediaTrackData)

        prevMediaTrackData = searchHelper?.getPrev(mediaTrackData)
        val prevMediaItem = prevMediaTrackData?.uri?.let { MediaItem.fromUri(it) }
        if (prevMediaItem != null) {
            mediaController?. addMediaItem(0, prevMediaItem)
            offset = 1
        }

        val mediaItem = mediaTrackData.uri?.let { MediaItem.fromUri(it) }
        mediaItem?.let {
            mediaController?.addMediaItem(it)
        }

        nextMediaTrackData = searchHelper?.getNext(mediaTrackData)
        val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }
        if (nextMediaItem != null) {
            mediaController?.addMediaItem(nextMediaItem)
        }


        val count = mediaController?.mediaItemCount
        if (count != null) {
            mediaController?.seekTo(offset,0)
//            play()
        }

    }

}