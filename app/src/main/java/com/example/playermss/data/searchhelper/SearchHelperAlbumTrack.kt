package com.example.playermss.data.searchhelper

import androidx.media3.common.Player
import com.example.playermss.data.MediaTrackData


class SearchHelperAlbumTrack(
    private val groupedSortedTracks: List<Pair<Pair<String, String>, List<MediaTrackData>>>) : SearchHelper {

    override var overrunTop = false
    override var overrunBottom = false

    override var repeatMode = Player.REPEAT_MODE_OFF

    override fun getMediaTrackDataForCode(code: Int): MediaTrackData? {
        groupedSortedTracks.forEach { album ->
            album.second.forEach { trackData ->
                if (trackData.testHash(code)) {
                    return trackData
                }
            }
        }
        return null
    }

    private fun getFirstTrack(): MediaTrackData? {
        if(groupedSortedTracks.isNotEmpty()) {
            val firstAlbum =  groupedSortedTracks[0].second
            val firstSong = firstAlbum.first()
            overrunBottom = true
            return firstSong
        }else{
            return null
        }
    }

    private fun getLastTrack(): MediaTrackData? {
        if(groupedSortedTracks.isNotEmpty()) {
            val lastAlbum = groupedSortedTracks.last().second
            val lastSong = lastAlbum.last()
            overrunTop = true
            return lastSong
        }else{
            return null
        }
    }

    private fun getLastTrackFromPrevAlbum(mediaTrackData: MediaTrackData?): MediaTrackData?{
        if(groupedSortedTracks.isNotEmpty()) {
            val currentAlbum = groupedSortedTracks.find { it.first.first == mediaTrackData?.albumKey() }
            val prevAlbumIndex = groupedSortedTracks.indexOf(currentAlbum) - 1
            if(prevAlbumIndex >= 0){
                val lastTrackIndex = groupedSortedTracks[prevAlbumIndex].second.count() - 1
                return groupedSortedTracks[prevAlbumIndex].second[lastTrackIndex]
            } else {
                return getLastTrack()
            }
        }
        return null
    }

    private fun getFirstTrackFromNextAlbum(mediaTrackData: MediaTrackData?): MediaTrackData?{
        if(groupedSortedTracks.isNotEmpty()) {
            val currentAlbum = groupedSortedTracks.find { it.first.first == mediaTrackData?.albumKey() }
            val nextAlbumIndex = groupedSortedTracks.indexOf(currentAlbum) + 1
            return if(nextAlbumIndex < groupedSortedTracks.count()){
                groupedSortedTracks[nextAlbumIndex].second[0]
            }else{
                getFirstTrack()
            }
        }
        return null
    }

    override fun getPrev(mediaTrackData: MediaTrackData?): MediaTrackData?{
            val currentAlbum = groupedSortedTracks.find { it.first.first == mediaTrackData?.albumKey() }
        if(currentAlbum != null){
            val currentAlbumIndex = groupedSortedTracks.indexOf(currentAlbum)
            val currentTracks = groupedSortedTracks[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if( currentTrackIndex - 1 >= 0) {
                currentTracks[currentTrackIndex - 1]
            } else {
                getLastTrackFromPrevAlbum(mediaTrackData)
                    ?: if (repeatMode == Player.REPEAT_MODE_ALL) {
                        getLastTrack()
                    } else {
                        null
                    }
            }
        }
        return null
    }

    override fun getNext(mediaTrackData: MediaTrackData?): MediaTrackData? {
        val currentAlbum = groupedSortedTracks.find { it.first.first == mediaTrackData?.albumKey() }
        if (currentAlbum != null) {
            val currentAlbumIndex = groupedSortedTracks.indexOf(currentAlbum)
            val currentTracks = groupedSortedTracks[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if (currentTrackIndex + 1 < currentTracks.count()) {
                currentTracks[currentTrackIndex + 1]
            } else {
                getFirstTrackFromNextAlbum(mediaTrackData)
                    ?: if (repeatMode == Player.REPEAT_MODE_ALL) {
                        getFirstTrack()
                    } else {
                        null
                    }
            }
        }
        return null
    }

}
