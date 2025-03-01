package com.example.playermss.data

import android.app.RecoverableSecurityException
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
import androidx.annotation.RequiresExtension
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
import kotlinx.coroutines.delay
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
    val year: String = ("0"),
    val track: String = ("0"),
    val uri: Uri? = null,
)


class MediaViewModel: ViewModel() {

    private val _mediaControllerLoaded = MutableStateFlow(false)
    val mediaControllerLoaded: StateFlow<Boolean> = _mediaControllerLoaded.asStateFlow()

    private val _mediaStoreGenerations = MutableStateFlow(listOf<Long>())
    val mediaStoreGenerations: StateFlow<List<Long>> = _mediaStoreGenerations.asStateFlow()

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
    private lateinit var tracksAsList: List<MediaTrackData>

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
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

                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)

//                        events.
                    }
                }
            )

            _mediaControllerLoaded.value = true

        }, MoreExecutors.directExecutor())

        viewModelScope.launch {
            // Coroutine that will be canceled when the ViewModel is cleared.
            while (true) {
                reCheckMediaStoreGeneration()
                delay(10000)
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun reCheckMediaStoreGeneration(){
        val externalVolumeNames = MediaStore.getExternalVolumeNames(context)
        val generations = externalVolumeNames.map{MediaStore.getGeneration(context,it)}

        if(_mediaStoreGenerations.value != generations){
            _mediaStoreGenerations.value = generations
        }

    }

    private fun yearFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): String {

        val patterns= arrayOf("(19|20)\\d{2}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])\\.(1[0-2]|0[1-9]|[1-9])\\.[0-9]{2}",
        )

        var year = columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getString(it) }.toString()

        if (year == "null") {
            val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
                .toString()

            for (pattern in patterns) {
                val yearPattern = Regex(pattern)
                val res = yearPattern.find(data)
                if (res != null) {
                    year = res.value
                    return year
                }
            }
        }
        return year
    }

    private fun trackNumberFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): String {

        val patterns= arrayOf("/d{2}",
            "/[0-9]{2}",
        )

        var trackNumber = columnMap[MediaStore.Audio.AudioColumns.TRACK]?.let { cursor.getString(it) }.toString()

        if (trackNumber == "null") {
            val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
                .toString()

            for (pattern in patterns) {
                val yearPattern = Regex(pattern)
                val res = yearPattern.findAll(data)
                if (res.count() > 0) {
                    trackNumber = res.last().value//todo remove leading /
                    return trackNumber
                }
            }
        }
        return trackNumber
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun uriFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Uri?{
        val id = columnMap[MediaStore.Audio.AudioColumns._ID]?.let { cursor.getLong(it) }
        val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }

        val externalVolumeNames = MediaStore.getExternalVolumeNames(context)//.map { it.uppercase(context.resources.configuration.locales[0]) }

        if(data != null){
            val externalVolumeName = externalVolumeNames.filter { data.uppercase(context.resources.configuration.locales[0])
                .contains(
                it.uppercase(context.resources.configuration.locales[0])
            ) }

            if(externalVolumeName.count() == 1){

                val volumeName = externalVolumeName[0]

                if(volumeName != null){

                    val contentUri = MediaStore.Audio.Media.getContentUri(volumeName)
                    val itemUri = id?.let { ContentUris.withAppendedId(contentUri, it) }

                    val mediaItem: MediaItem= MediaItem.fromUri(Uri.parse(data))
                    val mediaItem2: MediaItem?= itemUri?.let { MediaItem.fromUri(it) }
//    if(id != null && data != null){
//        return
//    }
                    return itemUri

                }
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun cursorToMediaTrackData(cursor: Cursor, columnMap: Map<String,Int>): MediaTrackData{

        val rv = MediaTrackData(
            columnMap[MediaStore.Audio.AudioColumns.ARTIST]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.ALBUM]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.TITLE]?.let { cursor.getString(it) }.toString(),
            yearFromCursor(cursor, columnMap),
            trackNumberFromCursor(cursor, columnMap),
            uri = uriFromCursor(cursor, columnMap)
        )
        return rv
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
    }

    fun toggleAlbumExpanded(name: String){
        _expandedAlbums.value = toggleInSet(_expandedAlbums.value, name)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cursorToTrackList(cursor: Cursor): List<MediaTrackData>{
        val audioColumnIds = audioColumns.associateWith { cursor.getColumnIndex(it) }

        val list = (1 .. cursor.count).map {
            cursor.moveToNext()
            cursorToMediaTrackData(cursor, audioColumnIds)
        }

        return list
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateQueryStatistic(list: List<MediaTrackData>){

        val mapByArtist = list.groupBy { it.artist }.toList().sortedBy { it.first }.toMap()
        val mapByArtistAlbumSortedByYear = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album }.toList().sortedBy { (key,value) -> value[0].year+key }.toMap() }
        val mapByArtistAlbumSortedByYearTrack=mapByArtistAlbumSortedByYear.entries.associate { it.key to it.value.entries.associate {it1 -> it1.key to it1.value.sortedBy { mtd -> mtd.track } } }
        _searchResults.value = mapByArtistAlbumSortedByYearTrack

        tracksAsList = mapByArtistAlbumSortedByYearTrack.flatMap { it.value.flatMap { it1 -> it1.value } }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams(), contentUri: Uri): Cursor?{

        var cursor: Cursor? = null

//        viewModelScope.launch {
            cursor = this@MediaViewModel.context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs,
                queryParams?.sortOrder
            )
            Log.d("MVM", "Items: ${cursor?.count}")
//        }
        return cursor
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(searchFields : List<TextFieldViewModel>){
        viewModelScope.launch {

            val queryParams = QueryParams()

            queryParams.selection = "${MediaStore.Audio.Media.ARTIST} like ? and " +
                    "${MediaStore.Audio.Media.ALBUM} like ? and " +
                    " ${MediaStore.Audio.Media.TITLE} like ?"

            queryParams.selectionArgs = arrayOf(
                "%${searchFields[0].textField.value}%",
                "%${searchFields[1].textField.value}%",
                "%${searchFields[2].textField.value}%"
            )


            val trackList = mutableListOf<MediaTrackData>()
            val externalVolumeNames = MediaStore.getExternalVolumeNames(context)

            for (name in externalVolumeNames) {
                val contentUri = MediaStore.Audio.Media.getContentUri(name)

                val cursor = query(queryParams, contentUri)
                cursor?.let {
                    trackList += cursorToTrackList(cursor)
                    cursor.close()
                }
            }
            updateQueryStatistic(trackList)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun removeQuery(searchFields : List<TextFieldViewModel>) {
        val queryParams = QueryParams()

        queryParams.selection = "${MediaStore.Audio.Media.ARTIST} like ? and " +
                "${MediaStore.Audio.Media.ALBUM} like ? and " +
                " ${MediaStore.Audio.Media.TITLE} like ?"

        queryParams.selectionArgs = arrayOf(
            "%${searchFields[0].textField.value}%",
            "%${searchFields[1].textField.value}%",
            "%${searchFields[2].textField.value}%"
        )


        viewModelScope.launch {
            val externalVolumeNames = MediaStore.getExternalVolumeNames(context)

            for (name in externalVolumeNames) {
                val contentUri = MediaStore.Audio.Media.getContentUri(name)
                try {
                    // "w" for write.
                    val removed = this@MediaViewModel.context.contentResolver.delete(
                        contentUri,
                        queryParams.selection,
                        queryParams.selectionArgs,
                    )
                    Log.d("MVM", "Items removed: $removed")

                } catch (securityException: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = securityException as?
                                RecoverableSecurityException ?:
                        throw RuntimeException(securityException.message, securityException)

//                        val intentSender =
//                            recoverableSecurityException.userAction.actionIntent.intentSender
//                        intentSender?.let {
//                            startIntentSenderForResult(intentSender, image-request-code
//
//                                ,
//                                null, 0, 0, 0, null)
                      //  }
                    } else {
                        throw RuntimeException(securityException.message, securityException)
                    }
                }

            }

        }
    }

    fun play(mediaTrackData: MediaTrackData){
        Log.d("DTP","${mediaTrackData.title}")
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