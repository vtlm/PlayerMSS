package com.example.playermss.data

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//data class ParamDescription(
//    val name: String = "name"
//)

data class SearchStatistic(
    val artists: Int = 0,
    val albums: Int = 0,
    val titles: Int = 0,
)


val audioColumns = arrayOf(MediaStore.Audio.AudioColumns.ARTIST,
    MediaStore.Audio.AudioColumns.ALBUM,
    MediaStore.Audio.AudioColumns.TITLE,
    MediaStore.Audio.AudioColumns.YEAR,
    MediaStore.Audio.AudioColumns.TRACK,
    MediaStore.Audio.AudioColumns.DURATION,
    )

data class MediaTrackData(
    val artist: String = (""),
    val album: String = (""),
    val title: String = (""),
    val year: Int = 0,
)

fun cursorToMediaTrackData(cursor: Cursor, columnMap: Map<String,Int?>): MediaTrackData{
    return MediaTrackData(
        columnMap[MediaStore.Audio.AudioColumns.ARTIST]?.let { cursor.getString(it) }.toString(),
        columnMap[MediaStore.Audio.AudioColumns.ALBUM]?.let { cursor.getString(it) }.toString(),
        columnMap[MediaStore.Audio.AudioColumns.TITLE]?.let { cursor.getString(it) }.toString(),
        columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getString(it) }!!.toInt(),
    )
}

class MediaViewModel: ViewModel() {

    private val _cursor = MutableStateFlow<Cursor?>(null)
    val cursor: StateFlow<Cursor?> = _cursor.asStateFlow()

    private val _searchStatistic = MutableStateFlow(SearchStatistic())
    val searchStatistic: StateFlow<SearchStatistic> = _searchStatistic.asStateFlow()

    private var mapByArtistAlbumExpandable: MutableMap<String, Pair<MutableMap<String, Pair<List<MediaTrackData>, Boolean>>, Boolean>> = mutableMapOf()

    private val _searchResults = MutableStateFlow<Map<String, Pair<Map<String, Pair<List<MediaTrackData>, Boolean>>, Boolean>>>(mapOf())
    val searchResults : StateFlow<Map<String, Pair<Map<String, Pair<List<MediaTrackData>, Boolean>>, Boolean>>> = _searchResults.asStateFlow()

    private  var _updateCounter = MutableStateFlow(0)
    val updateCounter = _updateCounter.asStateFlow()

//    private var __paramStrings = mutableListOf<String>("","","","",)
//    private val _paramStrings = MutableStateFlow(__paramStrings)
//    val paramStrings: MutableStateFlow<MutableList<String>> = _paramStrings//.asStateFlow()
//
//    val paramDescriptions = listOf(ParamDescription("Title"),
//        ParamDescription("Artist"),
//        ParamDescription("Album"),
//        ParamDescription("Year"),
//        )

    lateinit var context: Context


//    fun setParamString(ind: Int, value: String){
//        __paramStrings[ind] = value
//        __paramStrings = __paramStrings.subList(0,3)
//    }

    fun toggleArtistExpanded(key: String){
        val artistVal = mapByArtistAlbumExpandable[key]
        val newArtistVal = artistVal?.copy(artistVal.first,  ! artistVal.second)
        if(newArtistVal != null) {
            mapByArtistAlbumExpandable[key] = newArtistVal
        }

//        _updateCounter.value += 1
        _searchResults.value = mapByArtistAlbumExpandable. toMutableMap()
        Log.d("DBG","artist expand: ${artistVal?.second} map: ${System.identityHashCode(_searchResults.value)}")
    }

    private fun updateQueryStatistic(){

        val cursor = _cursor.value

        val audioColumnId = audioColumns.associateWith { cursor?.getColumnIndexOrThrow(it) }

        if (cursor != null) {

            val list = (1 .. cursor.count).map {
                cursor.moveToNext()
                cursorToMediaTrackData(cursor, audioColumnId)
            }

            val mapByArtist = list.groupBy { it.artist }//.mapValues { Pair(it.value, true) }
            val mapByArtistExpandable = mapByArtist.mapValues { Pair(it.value, true) }.toMutableMap()

            //example
            //val mapByArtistAlbum = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album } }
            val _mapByArtistAlbumExpandable = mapByArtistExpandable.entries.associate{ it.key to Pair(it.value.first.groupBy { it1 -> it1.album }.mapValues { it2 -> Pair(it2.value,true) }.toMutableMap(), it.value.second) }
                //listByArtist.map { it -> Pair(it.key, Pair(it.value.first.value.groupBy { it.album }.mapValues { Pair(it,true) },it.value.second)) }
            mapByArtistAlbumExpandable = _mapByArtistAlbumExpandable.toMutableMap()

            _searchResults.value = mapByArtistAlbumExpandable
//            val listByArtistAlbum = listByArtist.map { it -> it.value.groupBy { it.album } }

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams()){

        viewModelScope.launch {
            val f1 = MediaStore.getExternalVolumeNames(context)
            val contentUri = MediaStore.Audio.Media.getContentUri(f1.elementAt(0))

            _cursor.value = context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs,
                queryParams?.sortOrder
            )

            Log.d("MVM", "Items: ${_cursor.value?.count}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(searchFields : List<TextFieldViewModel>){
        val queryParams = QueryParams()

        queryParams.selection = "${MediaStore.Audio.Media.ARTIST} like ? and " +
                "${MediaStore.Audio.Media.ALBUM} like ? and " +
                " ${MediaStore.Audio.Media.TITLE} like ?"

        queryParams.selectionArgs = arrayOf("%${searchFields[0].textField.value}%",
            "%${searchFields[1].textField.value}%",
            "%${searchFields[2].textField.value}%")

        query(queryParams)
        updateQueryStatistic()
    }

}