package com.example.playermss.data

import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.PlaybackService
import com.example.playermss.TrackList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


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

    private val _searchResults = MutableStateFlow<Map<String, Map<String, List<MediaTrackData>>>>(mapOf())
    val searchResults : StateFlow<Map<String, Map<String, List<MediaTrackData>>>> = _searchResults.asStateFlow()

    private var _expandedArtists = MutableStateFlow<Set<String>>(setOf())
    val expandedArtists: StateFlow<Set<String>> = _expandedArtists.asStateFlow()

    private var _expandedAlbums = MutableStateFlow<Set<String>>(setOf())
    val expandedAlbums: StateFlow<Set<String>> = _expandedAlbums.asStateFlow()

    lateinit var context: Context


    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    fun init(){
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )
        controllerFuture =
            MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // MediaController is available here with controllerFuture.get()
            mediaController = controllerFuture?.get()

//            visualizer = Visualizer(sessionToken.uid)
//            bassBoost = BassBoost(0,sessionToken.uid)

            mediaController?.addListener(
                object : Player.Listener {

                    override fun onPlayerError(error: PlaybackException) {
                        val cause = error.cause
                        if (cause is HttpDataSource.HttpDataSourceException) {
                            // An HTTP error occurred.
                            val httpError = cause
                            // It's possible to find out more about the error both by casting and by querying
                            // the cause.
                            if (httpError is HttpDataSource.InvalidResponseCodeException) {
                                // Cast to InvalidResponseCodeException and retrieve the response code, message
                                // and headers.
                            } else {
                                // Try calling httpError.getCause() to retrieve the underlying cause, although
                                // note that it may be null.
                            }
                        }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        Log.d("DMG", "mediaData changed")
                    }
                }
            )

//            mediaControllerLoaded.value = true

        }, MoreExecutors.directExecutor())

    }


    private fun toggleInSet(inSet: Set<String>, name: String) : Set<String>{

        var cSet = inSet//_expandedArtists.value

        cSet = if(cSet.contains(name)){
            cSet.minus(name)
        }else{
            cSet.plus(name)
        }

        Log.d("DBG","artist expand: ${cSet.count()} ${System.identityHashCode(_expandedArtists.value)}")
        return cSet
    }

    fun toggleArtistExpanded(name: String){
        _expandedArtists.value = toggleInSet(_expandedArtists.value, name)
    }

    fun toggleAlbumExpanded(name: String){
        _expandedAlbums.value = toggleInSet(_expandedAlbums.value, name)
    }

    private fun updateQueryStatistic(){

        val cursor = _cursor.value

        val audioColumnId = audioColumns.associateWith { cursor?.getColumnIndexOrThrow(it) }

        if (cursor != null) {

            val list = (1 .. cursor.count).map {
                cursor.moveToNext()
                cursorToMediaTrackData(cursor, audioColumnId)
            }

            val mapByArtist = list.groupBy { it.artist }
            val mapByArtistAlbum = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album } }
            _searchResults.value = mapByArtistAlbum
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