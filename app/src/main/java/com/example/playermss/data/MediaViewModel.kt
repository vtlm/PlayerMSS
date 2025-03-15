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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.Messages
import com.example.playermss.PlaybackService
import com.example.playermss.imageBitmapFromBytes
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.min


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
    val year: Int? = 0,
    val track: Int? = 0,
    val duration: Int? = 0,
    val uri: Uri? = null,
)

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository
): ViewModel() {

    private val _mediaControllerLoaded = MutableStateFlow(false)
    val mediaControllerLoaded: StateFlow<Boolean> = _mediaControllerLoaded.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressTitle = MutableStateFlow("")
    val progressTitle: StateFlow<String> = _progressTitle.asStateFlow()

    private val _playingItemId = MutableStateFlow(0)
    val playingItemId: StateFlow<Int> = _playingItemId.asStateFlow()

    private val _mediaStoreGenerations = MutableStateFlow(listOf<Long>())
    val mediaStoreGenerations: StateFlow<List<Long>> = _mediaStoreGenerations.asStateFlow()

    private val _currentTrackMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentTrackMediaMetadata: StateFlow<MediaMetadata?> = _currentTrackMediaMetadata.asStateFlow()

    private val _searchStatistic = MutableStateFlow(SearchStatistic())
    val searchStatistic: StateFlow<SearchStatistic> = _searchStatistic.asStateFlow()

    val tracksList: StateFlow<List<MediaTrackData>> = userDataRepository.asStateFlow(viewModelScope)

    private val _searchResults = MutableStateFlow<Map<String, Map<String, List<MediaTrackData>>>>(mapOf())
    val searchResults: StateFlow<Map<String, Map<String, List<MediaTrackData>>>> = _searchResults.asStateFlow()

    private val _querySortedResults = MutableStateFlow(sortByArtistAlbumAsPairs(tracksList.value))
    val querySortedResults: StateFlow<List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>> = _querySortedResults.asStateFlow()

    private var _expandedArtists = MutableStateFlow<Set<String>>(setOf())
    val expandedArtists: StateFlow<Set<String>> = _expandedArtists.asStateFlow()

    private var _expandedAlbums = MutableStateFlow<Set<String>>(setOf())
    val expandedAlbums: StateFlow<Set<String>> = _expandedAlbums.asStateFlow()

    private var _sleevePicture = MutableStateFlow<ImageBitmap?>(null)
    val sleevePicture: StateFlow<ImageBitmap?> = _sleevePicture.asStateFlow()

    val queryFields = arrayOf(QueryTextField("Artist",userPreferencesRepository,viewModelScope),
        QueryTextField("Album",userPreferencesRepository,viewModelScope),
        QueryTextField("Title",userPreferencesRepository,viewModelScope),
        QueryTextField("FromYear",userPreferencesRepository,viewModelScope),
        QueryTextField("ToYear",userPreferencesRepository,viewModelScope),)

    lateinit var context: Context

    private var bassBoost: BassBoost? = null
    private var visualizer: Visualizer? = null

    lateinit var searchHelper: SearchHelper
    private var currentMediaTrackData: MediaTrackData? = null
    private var prevMediaTrackData: MediaTrackData? = null
    private var nextMediaTrackData: MediaTrackData? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
    private lateinit var tracksAsList: List<MediaTrackData>


    val isRemainTime: StateFlow<Boolean> =
        userPreferencesRepository.isRemainTime.map { isRemainTime ->
            isRemainTime
        }.stateIn(scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = runBlocking {
                userPreferencesRepository.isRemainTime.first()
            }
        )

    fun setRemainTime(isRemainTime: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveRemainTimePreference(isRemainTime)
        }
    }

    val playerRepeatMode: StateFlow<Int> =
        userPreferencesRepository.playerRepeatMode.map { playerRepeatMode ->
            playerRepeatMode
        }.stateIn(scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = runBlocking {
                userPreferencesRepository.playerRepeatMode.first()
            }
        )

    fun setPlayerRepeatMode(playerRepeatMode: Int) {
        viewModelScope.launch {
            userPreferencesRepository.savePlayerRepeatModePreference(playerRepeatMode)
        }
    }

    fun incPlayerRepeatMode() {
        var nextPlayerRepeatMode = playerRepeatMode.value + 1
        if(nextPlayerRepeatMode > Player.REPEAT_MODE_ALL){
            nextPlayerRepeatMode = Player.REPEAT_MODE_OFF
        }
        setPlayerRepeatMode(nextPlayerRepeatMode)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    fun init(){

        _progressTitle.value = "Loading MediaSession"

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
//                        _currentTrackMediaMetadata.value = mediaMetadata
                        Log.d("DMG", "mediaData changed")
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)

                        if(reason == 1){ //transition to next
                            prevMediaTrackData = currentMediaTrackData
                            currentMediaTrackData = nextMediaTrackData

                            expandArtistAlbumFor(currentMediaTrackData)

                            _playingItemId.value = System.identityHashCode(currentMediaTrackData)
                            nextMediaTrackData = searchHelper?.getNext(nextMediaTrackData)
                            val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }
                            if (nextMediaItem != null) {
                                mediaController?.removeMediaItem(0)
                                mediaController?.addMediaItem(nextMediaItem)
                            }

                        }

                        if(reason == 2){ //seek to prev, next buttons

                            val mediaItemsCount = mediaController?.mediaItemCount
                            if(mediaItemsCount != null) {
                                val firstMediaItem = mediaController?.getMediaItemAt(0)
//prev
                                if (mediaItem == firstMediaItem) {
                                    nextMediaTrackData = currentMediaTrackData
                                    currentMediaTrackData = prevMediaTrackData

                                    expandArtistAlbumFor(currentMediaTrackData)

                                    _playingItemId.value = System.identityHashCode(currentMediaTrackData)

                                    prevMediaTrackData = searchHelper?.getPrev(prevMediaTrackData)
                                    val prevMediaItem = prevMediaTrackData?.uri?.let { MediaItem.fromUri(it) }

                                    if (prevMediaItem != null) {

//                                        val c = mutableListOf<MediaItem?>()
//                                        for (m in 0..<mediaController?.mediaItemCount!!) {
//                                            c += mediaController?.getMediaItemAt(m)
//                                        }

                                        mediaController?.addMediaItem(0, prevMediaItem)

//                                        val c2 = mutableListOf<MediaItem?>()
//                                        for (m in 0..<mediaController?.mediaItemCount!!) {
//                                            c2 += mediaController?.getMediaItemAt(m)
//                                        }

                                        val cn = mediaController?.mediaItemCount

                                        if (cn == 4) {
                                            mediaController?.removeMediaItem(3)
                                        }
                                    }else{
                                        val cn = mediaController?.mediaItemCount
                                        if(cn != null) {
                                            mediaController?.removeMediaItem(cn - 1)
                                        }
                                    }
                                }else{
//next
                                    val lastIemIndex = mediaItemsCount - if (mediaItemsCount > 0) 1 else 0
                                    val lastMediaItem = mediaController?.getMediaItemAt(lastIemIndex)

                                    if (mediaItem == lastMediaItem) {

                                        if (nextMediaTrackData != null) {

                                            prevMediaTrackData = currentMediaTrackData
                                            currentMediaTrackData = nextMediaTrackData

                                            expandArtistAlbumFor(currentMediaTrackData)
                                            _playingItemId.value = System.identityHashCode(currentMediaTrackData)

                                            nextMediaTrackData = searchHelper?.getNext(nextMediaTrackData)

                                            if (nextMediaTrackData != null) {
                                                val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }
                                                if (nextMediaItem != null) {
                                                    mediaController?.addMediaItem(nextMediaItem)

                                                    val cn = mediaController?.mediaItemCount
                                                    if (cn == 4) {
                                                        mediaController?.removeMediaItem(0)
                                                    }
                                                }
                                            }else{
                                                mediaController?.removeMediaItem(0)
                                            }



                                        }

                                    }

                                }
                            }
                        }
                    }

//                    override fun onPlaybackStateChanged(playbackState: Int) {
//                        super.onPlaybackStateChanged(playbackState)
//                        if(playbackState == Player.STATE_READY){
//                            val md = mediaController.mediaMetadata()
//                        }
//                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)

                        if(events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)){
                            if(player.playbackState == Player.STATE_READY){
                                _currentTrackMediaMetadata.value = player.mediaMetadata
                                _sleevePicture.value = player.mediaMetadata.artworkData?.let {
                                    imageBitmapFromBytes(it)
                                }
                            }
                        }

                        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                            if (player.playbackState == Player.STATE_READY) {
                                _currentTrackMediaMetadata.value = player.mediaMetadata
                                _sleevePicture.value = player.mediaMetadata.artworkData?.let {
                                    imageBitmapFromBytes(it)
                                }
                            }
                        }
                    }

                }
            )

            _mediaControllerLoaded.value = true
            mediaController?.repeatMode = playerRepeatMode.value
            _progressTitle.value = ""

        }, MoreExecutors.directExecutor())

//        viewModelScope.launch {
//            // Coroutine that will be canceled when the ViewModel is cleared.
//            while (true) {
//                reCheckMediaStoreGeneration()
//                delay(10000)
//            }
//        }

        viewModelScope.launch {
            playerRepeatMode.collect {
//                while (mediaController == null){//startup case
//                    delay(200)
//                }
                mediaController?.repeatMode = it
            }
        }

        viewModelScope.launch {
            querySortedResults.collect{
                searchHelper = SearchHelper(it)
            }

        }
    }

    fun play(){
        if(mediaController != null){
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    fun pause(){
        if(mediaController != null){
            mediaController?.pause()
        }
    }

    private fun expandArtistAlbumFor(mediaTrackData: MediaTrackData?){
        if (!_expandedAlbums.value.contains(mediaTrackData?.album)) {
            _expandedAlbums.value += mediaTrackData?.album.toString()
        }
        if (!_expandedArtists.value.contains(mediaTrackData?.artist)) {
            _expandedArtists.value += mediaTrackData?.artist.toString()
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

    private fun yearFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Int? {

        val patterns= arrayOf("(19|20)\\d{2}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
            "(3[01]|[12][0-9]|0[1-9]|[1-9])\\.(1[0-2]|0[1-9]|[1-9])\\.[0-9]{2}",
        )

        var year = columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getInt(it) }

        if (year == null || year == 0) {
            val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
                .toString()

            for (pattern in patterns) {
                val yearPattern = Regex(pattern)
                val res = yearPattern.find(data)
                if (res != null) {
                    year = res.value.toIntOrNull()
                    return year
                }
            }
        }
        return year
    }

    private fun trackNumberFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Int? {

        val patterns= arrayOf("d{2}",
            //"\\s/d{2}\\s",
            "[0-9]{2}",
           // "\\s/[0-9]{2}\\s",
        )

        var trackNumber = columnMap[MediaStore.Audio.AudioColumns.TRACK]?.let { cursor.getInt(it) }

        if (trackNumber == null || trackNumber == 0) {
            val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
                .toString().split('/').last()

            for (pattern in patterns) {
                val res = Regex(pattern).findAll(data)
                if (res.count() > 0) {
                    trackNumber = res.last().value.toIntOrNull()
                    return trackNumber
                }else{
                    trackNumber = null
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

//        val dt = columnMap[MediaStore.Audio.AudioColumns.DURATION]?.let { cursor.getType(it) }
//        val dt1 = columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getType(it) }
//        val dt2 = columnMap[MediaStore.Audio.AudioColumns.TRACK]?.let { cursor.getType(it) }

        val rv = MediaTrackData(
            columnMap[MediaStore.Audio.AudioColumns.ARTIST]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.ALBUM]?.let { cursor.getString(it) }.toString(),
            columnMap[MediaStore.Audio.AudioColumns.TITLE]?.let { cursor.getString(it) }.toString(),
            yearFromCursor(cursor, columnMap),
            trackNumberFromCursor(cursor, columnMap),
            columnMap[MediaStore.Audio.AudioColumns.DURATION]?.let { cursor.getInt(it) },
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

        val list = (1 .. min(cursor.count,Int.MAX_VALUE)).map {
            cursor.moveToNext()
            cursorToMediaTrackData(cursor, audioColumnIds)
        }

        return list
    }

    private fun sortByArtistAlbumAsPairs(list: List<MediaTrackData>):  List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>{
        val setByArtist = list.groupBy { it.artist }.toList().toSortedSet(compareBy { it.first })
        val listByArtistAlbum = setByArtist.map { Pair(it.first,it.second.groupBy { it1 -> it1.album }.toList().toSortedSet(
            compareBy { it2 -> it2.second[0].year.toString() + it2.first }
        )) }
        val listByArtistAlbumTrack = listByArtistAlbum.map { Pair(it.first,it.second.map { it2 -> Pair(it2.first, it2.second.sortedBy{ mtd -> mtd.track})}) }
        return listByArtistAlbumTrack
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateQueryStatistic(list: List<MediaTrackData>){

        viewModelScope.launch {
            userDataRepository.saveTrackList(list)
        }

        val sortedList = sortByArtistAlbumAsPairs(list)
        _querySortedResults.value = sortedList
//        searchHelper = SearchHelper(sortedList)

        val mapByArtist = list.groupBy { it.artist }.toList().sortedBy { it.first }.toMap()
//        val mapByArtistEmpty = mapByArtist.entries.associate { it.key to mapOf(Pair("",listOf<MediaTrackData>())) }
        val mapByArtistAlbumSortedByYear = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album }.toList().sortedBy { (key,value) -> value[0].year.toString() + key }.toMap() }
        val mapByArtistAlbumSortedByYearTrack=mapByArtistAlbumSortedByYear.entries.associate { it.key to it.value.entries.associate {it1 -> it1.key to it1.value.sortedBy { mtd -> mtd.track } } }
        _searchResults.value = mapByArtistAlbumSortedByYearTrack
//        _searchResults.value = mapByArtistEmpty

        tracksAsList = mapByArtistAlbumSortedByYearTrack.flatMap { it.value.flatMap { it1 -> it1.value } }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams(), contentUri: Uri): Cursor?{

        val cursor: Cursor? = this@MediaViewModel.context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs,
                queryParams?.sortOrder
            )
//            Log.d("MVM", "Items: ${cursor?.count}")
//        }
        return cursor
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(){
        viewModelScope.launch(Dispatchers.Default) {

            _progressTitle.value = "Querying MediaStore"
            queryFields.forEach { it.saveText() }

            val queryParams = QueryParams()
            queryParams.projection = audioColumns

            queryParams.selection = "${MediaStore.Audio.Media.ARTIST} like ? and " +
                    "${MediaStore.Audio.Media.ALBUM} like ? and " +
                    " ${MediaStore.Audio.Media.TITLE} like ?"

            queryParams.selectionArgs = arrayOf(
                "%${queryFields[0].text.value}%",
                "%${queryFields[1].text.value}%",
                "%${queryFields[2].text.value}%"
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
            _progressTitle.value = ""
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


        viewModelScope.launch(Dispatchers.Default) {
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
        Log.d("DTP", mediaTrackData.title)
        mediaController?.clearMediaItems()

        var offset = 0

        currentMediaTrackData = mediaTrackData
        _playingItemId.value = System.identityHashCode(mediaTrackData)

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
            play()
        }

    }

}