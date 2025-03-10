package com.example.playermss.data


class SearchHelper(private val groupedSortedTracks: List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>?){

    var currentArtist: String? = ""
    var currentAlbumKey: String? = ""

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
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.album }
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
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.album }
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
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.album }
            val currentAlbumIndex = currentAlbumsList.indexOf(currentAlbum)
            val currentTracks = currentAlbumsList[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if( currentTrackIndex - 1 >= 0) {
                currentTracks[currentTrackIndex - 1]
            } else {
                getLastTrackFromPrevAlbum(mediaTrackData)
            }
        }
        return null
    }

    fun getNext(mediaTrackData: MediaTrackData?): MediaTrackData?{
        val currentArtist = groupedSortedTracks?.find { it.first == mediaTrackData?.artist }
        val currentArtistIndex = groupedSortedTracks?.indexOf(currentArtist)
        if(groupedSortedTracks != null && currentArtistIndex != null && currentArtistIndex != -1) {
            val currentAlbumsList = groupedSortedTracks[currentArtistIndex].second
            val currentAlbum = currentAlbumsList.find { it.first == mediaTrackData?.album }
            val currentAlbumIndex = currentAlbumsList.indexOf(currentAlbum)
            val currentTracks = currentAlbumsList[currentAlbumIndex].second
            val currentTrackIndex = currentTracks.indexOf(mediaTrackData)
            return if( currentTrackIndex + 1 < currentTracks.count()) {
                currentTracks[currentTrackIndex + 1]
            } else {
                getFirstTrackFromNextAlbum(mediaTrackData)
            }
        }
        return null
    }

}
