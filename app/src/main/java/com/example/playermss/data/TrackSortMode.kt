package com.example.playermss.data

enum class TrackSortMode {
    ArtistAlbumTrack,
    AlbumTrack,
    Path

}

fun toSortMode(i: Int):TrackSortMode{
    return TrackSortMode.entries[i]
}
