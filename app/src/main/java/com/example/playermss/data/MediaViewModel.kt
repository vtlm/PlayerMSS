package com.example.playermss.data

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.audiofx.BassBoost
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.PlaybackService
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

val audioColumns = arrayOf(
    MediaStore.Audio.AudioColumns._ID,
    MediaStore.Audio.AudioColumns.DATA,
    MediaStore.Audio.AudioColumns.ARTIST,
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
    val uri: Uri? = null,
)


class MediaViewModel: ViewModel() {

    private val _mediaControllerLoaded = MutableStateFlow(false)
    val mediaControllerLoaded: StateFlow<Boolean> = _mediaControllerLoaded.asStateFlow()

    private val _currentTrackMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentTrackMediaMetadata: StateFlow<MediaMetadata?> = _currentTrackMediaMetadata.asStateFlow()

    private val _searchStatistic = MutableStateFlow(SearchStatistic())
    val searchStatistic: StateFlow<SearchStatistic> = _searchStatistic.asStateFlow()

    private val _searchResults = MutableStateFlow<Map<String, Map<String, List<MediaTrackData>>>>(mapOf())
    val searchResults : StateFlow<Map<String, Map<String, List<MediaTrackData>>>> = _searchResults.asStateFlow()

    private var _expandedArtists = MutableStateFlow<Set<String>>(setOf())
    val expandedArtists: StateFlow<Set<String>> = _expandedArtists.asStateFlow()

    private var _expandedAlbums = MutableStateFlow<Set<String>>(setOf())
    val expandedAlbums: StateFlow<Set<String>> = _expandedAlbums.asStateFlow()

    lateinit var context: Context

    private var bassBoost: BassBoost? = null
    private  var visualizer: Visualizer? = null


    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

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
                        _currentTrackMediaMetadata.value = mediaMetadata
                        Log.d("DMG", "mediaData changed")
                    }
                }
            )

            _mediaControllerLoaded.value = true

        }, MoreExecutors.directExecutor())

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun uriFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Uri?{
        val id = columnMap[MediaStore.Audio.AudioColumns._ID]?.let { cursor.getLong(it) }
        val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }

        val f1 = MediaStore.getExternalVolumeNames(context)
        val contentUri = MediaStore.Audio.Media.getContentUri(f1.elementAt(0))
        val itemUri = id?.let { ContentUris.withAppendedId(contentUri, it) }

        val mediaItem: MediaItem= MediaItem.fromUri(Uri.parse(data))
        val mediaItem2: MediaItem?= itemUri?.let { MediaItem.fromUri(it) }
//    if(id != null && data != null){
//        return
//    }
        return itemUri
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun cursorToMediaTrackData(cursor: Cursor, columnMap: Map<String,Int?>): MediaTrackData{
        return MediaTrackData(
            columnMap[MediaStore.Audio.AudioColumns.ARTIST]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.ALBUM]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.TITLE]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getString(it) }!!.toInt(),
            uri = uriFromCursor(cursor, columnMap)
        )
    }


    private fun toggleInSet(inSet: Set<String>, name: String) : Set<String>{
        return if(inSet.contains(name)){
            inSet.minus(name)
        }else{
            inSet.plus(name)
        }
    }

    fun toggleArtistExpanded(name: String){
        _expandedArtists.value = toggleInSet(_expandedArtists.value, name)
//        Log.d("DBG","artist expand: ${cSet.count()} ${System.identityHashCode(_expandedArtists.value)}")
    }

    fun toggleAlbumExpanded(name: String){
        _expandedAlbums.value = toggleInSet(_expandedAlbums.value, name)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateQueryStatistic(cursor: Cursor){

        val audioColumnIds = audioColumns.associateWith { cursor?.getColumnIndexOrThrow(it) }

        if (cursor != null) {

            val list = (1 .. cursor.count).map {
                cursor.moveToNext()
                cursorToMediaTrackData(cursor, audioColumnIds)
            }

            val mapByArtist = list.groupBy { it.artist }
            val mapByArtistAlbum = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album } }
            _searchResults.value = mapByArtistAlbum
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams()): Cursor?{

        var cursor: Cursor? = null

        viewModelScope.launch {
            val f1 = MediaStore.getExternalVolumeNames(context)//todo: check
            val contentUri = MediaStore.Audio.Media.getContentUri(f1.elementAt(0))//todo: check

            cursor = this@MediaViewModel.context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs,
                queryParams?.sortOrder
            )

            Log.d("MVM", "Items: ${cursor?.count}")
        }
        return cursor
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

        val cursor = query(queryParams)
        cursor?.let { updateQueryStatistic(it)
            cursor.close()
        }
    }

    fun play(mediaTrackData: MediaTrackData){
        val mediaItem = mediaTrackData.uri?.let { MediaItem.fromUri(it) }
        mediaItem?.let {
            mediaController?.setMediaItem(it)
            mediaController?.prepare()
            mediaController?.play()

//            val count = mediaController?.mediaItemCount
//            if (count != null) {
//                mediaController?.seekTo(count - 1,0)
//                mediaController?.play()
//            }

        }
    }

}