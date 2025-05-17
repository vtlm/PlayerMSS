package com.example.playermss.data

import androidx.media3.common.Player


class SearchHelper(private val groupedSortedTracks: List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>?){

//    var currentArtist: String? = ""
//    var currentAlbumKey: String? = ""

    var overrunTop = false
    var overrunBottom = false


    private var repeatMode = Player.REPEAT_MODE_OFF

    fun clearOverruns(){
        overrunTop = false
        overrunBottom = false
    }

    fun checkOverrunsFromTopToBottom():Boolean {
        var overrun = false
        if(overrunTop){
            overrunTop = false
            overrunBottom = true
            overrun = true
        }else{
            if(overrunBottom)
            {
                overrunBottom = false
            }
        }
        return overrun
    }

    fun checkOverrunsFromBottomToTop():Boolean {
        var overrun = false
        if(overrunBottom){
            overrunTop = true
            overrunBottom = false
            overrun = true
        }else{
            if(overrunTop){
                overrunTop = false
            }
        }
        return overrun
    }

    fun setRepeatMode(_repeatMode: Int){
        repeatMode = _repeatMode
    }

//    fun getMediaItem(mediaTrackData: MediaTrackData?):MediaItem?{
//        return mediaTrackData?.uri?.let { MediaItem.fromUri(it) }
//    }

    fun getMediaTrackDataForCode(code: Int): MediaTrackData?{
        groupedSortedTracks?.forEach { artistAlbums ->
            artistAlbums.second.forEach { album ->
                album.second.forEach { trackData ->
                    if(trackData.testHash(code)){
                        return  trackData
                    }
                }
            }
        }
        return null
    }

    private fun getFirstTrack(): MediaTrackData? {
        if(groupedSortedTracks != null) {
            val albumsList = groupedSortedTracks[0].second
            val firstAlbum = albumsList.first().second
            val firstSong = firstAlbum.first()
            overrunBottom = true
            return firstSong
        }else{
            return null
        }
    }

    private fun getLastTrack(): MediaTrackData? {
        if(groupedSortedTracks != null) {
            val albumsList = groupedSortedTracks.last().second
            val lastAlbum = albumsList.last().second
            val lastSong = lastAlbum.last()
            overrunTop = true
            return lastSong
        }else{
            return null
        }
    }

    private fun getLastTrackFromLastAlbumFromPrevArtist(mediaTrackData: MediaTrackData?): MediaTrackData?{
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)

        if(groupedSortedTracks != null && currentArtistIndex != null && currentArtistIndex > 0){
            val prevArtistIndex = currentArtistIndex - 1
            val albumsList = groupedSortedTracks[prevArtistIndex].second
            val lastAlbum = albumsList.last().second
            val lastTrack = lastAlbum.last()
            return lastTrack
        }
        return null
    }

    private fun getFirstSongFromFirstAlbumFromNextArtist(mediaTrackData: MediaTrackData?): MediaTrackData?{
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)

        if(groupedSortedTracks != null && currentArtistIndex != null && currentArtistIndex < groupedSortedTracks.count() - 1){
            val nextArtistIndex = currentArtistIndex + 1
            val albumsList = groupedSortedTracks[nextArtistIndex].second
            val firstAlbum = albumsList.first().second
            val firstSong = firstAlbum.first()
            return firstSong
        }
        return null
    }

    private fun getLastTrackFromPrevAlbum(mediaTrackData: MediaTrackData?): MediaTrackData?{
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.albumKey() }
            val prevAlbumIndex = currentAlbumsList.indexOf(currentAlbum) - 1
            if(prevAlbumIndex >= 0){
                val lastTrackIndex = currentAlbumsList[prevAlbumIndex].second.count() - 1
                return currentAlbumsList[prevAlbumIndex].second[lastTrackIndex]
            } else {
                return getLastTrackFromLastAlbumFromPrevArtist(mediaTrackData)
            }
        }
        return null
    }

    private fun getFirstTrackFromNextAlbum(mediaTrackData: MediaTrackData?): MediaTrackData?{
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.albumKey() }
            val nextAlbumIndex = currentAlbumsList.indexOf(currentAlbum) + 1
            if(nextAlbumIndex < currentAlbumsList.count()){
                return currentAlbumsList[nextAlbumIndex].second[0]
            }else{
                return getFirstSongFromFirstAlbumFromNextArtist(mediaTrackData)
            }
        }
        return null
    }

    fun getPrev(mediaTrackData: MediaTrackData?): MediaTrackData?{
//        overrunTop = false
//        overrunBottom =false
        //Log.d("TRD","prev for ${mediaTrackData?.title}")
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null && currentArtistIndex != -1) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.albumKey() }
            val currentAlbumIndex = currentAlbumsList.indexOf(currentAlbum)
            val currentTracks = currentAlbumsList[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if( currentTrackIndex - 1 >= 0) {
                currentTracks[currentTrackIndex - 1]
            } else {
                val prevTrack = getLastTrackFromPrevAlbum(mediaTrackData)
                if(prevTrack == null){
                    if(repeatMode == Player.REPEAT_MODE_ALL){
                        return getLastTrack()
                    }else {
                        return null
                    }
                }else{
                    return prevTrack
                }
            }
        }
        return null
    }

    fun getNext(mediaTrackData: MediaTrackData?): MediaTrackData?{
//        overrunTop = false
//        overrunBottom =false
        //Log.d("TRD","next for ${mediaTrackData?.title}")
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null && currentArtistIndex != -1) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.albumKey() }
            val currentAlbumIndex = currentAlbumsList.indexOf(currentAlbum)
            val currentTracks = currentAlbumsList[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if( currentTrackIndex + 1 < currentTracks.count()) {
                currentTracks[currentTrackIndex + 1]
            } else {
                val nextTrack = getFirstTrackFromNextAlbum(mediaTrackData)
                if(nextTrack == null){
                    if(repeatMode == Player.REPEAT_MODE_ALL){
                        return getFirstTrack()
                    }else {
                        return null
                    }
                }else{
                    return nextTrack
                }
            }
        }
        return null
    }

}
