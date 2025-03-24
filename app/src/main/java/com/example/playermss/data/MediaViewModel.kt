package com.example.playermss.data

import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.PlayTracksManager
import com.example.playermss.PlaybackService
import com.example.playermss.imageBitmapFromBytes
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min


data class SearchStatistic(
    val artists: Int = 0,
    val albums: Int = 0,
    val titles: Int = 0,
)

val TRACK_LIST_SCROLL_POS = intPreferencesKey("track_list_scroll_pos")
val PLAYING_ITEM_ID = intPreferencesKey("playing_item_id")

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository
): ViewModel(), DefaultLifecycleObserver {

    private val _mediaControllerLoaded = MutableStateFlow(false)
    val mediaControllerLoaded: StateFlow<Boolean> = _mediaControllerLoaded.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressTitle = MutableStateFlow("")
    val progressTitle: StateFlow<String> = _progressTitle.asStateFlow()

    private val _mediaStoreGenerations = MutableStateFlow(listOf<Long>())
    val mediaStoreGenerations: StateFlow<List<Long>> = _mediaStoreGenerations.asStateFlow()

    private val _currentTrackMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentTrackMediaMetadata: StateFlow<MediaMetadata?> = _currentTrackMediaMetadata.asStateFlow()

//    private val _searchStatistic = MutableStateFlow(SearchStatistic())
//    val searchStatistic: StateFlow<SearchStatistic> = _searchStatistic.asStateFlow()

    val tracksList: StateFlow<List<MediaTrackData>> = userDataRepository.asStateFlow(viewModelScope)

//    private val _searchResults = MutableStateFlow<Map<String, Map<String, List<MediaTrackData>>>>(mapOf())
//    val searchResults: StateFlow<Map<String, Map<String, List<MediaTrackData>>>> = _searchResults.asStateFlow()

    private val _querySortedResults = MutableStateFlow(sortByArtistAlbumAsPairs(tracksList.value))
    val querySortedResults: StateFlow<List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>> = _querySortedResults.asStateFlow()

    private var _magnitudes = MutableStateFlow(floatArrayOf())
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    val expandedArtists = StringSetDataStorePreferences("expandedArtists", userPreferencesRepository, viewModelScope)
    val expandedAlbums = StringSetDataStorePreferences("expandedAlbums", userPreferencesRepository, viewModelScope)

    val queryFields = arrayOf(
        QueryTextField("Artist", userPreferencesRepository, viewModelScope),
        QueryTextField("Album", userPreferencesRepository, viewModelScope),
        QueryTextField("Title", userPreferencesRepository, viewModelScope),
        QueryTextField("FromYear", userPreferencesRepository, viewModelScope),
        QueryTextField("ToYear", userPreferencesRepository, viewModelScope),
    )

    lateinit var context: Context

    private var _sleevePicture = MutableStateFlow<ImageBitmap?>(null)
    val sleevePicture: StateFlow<ImageBitmap?> = _sleevePicture.asStateFlow()

    private lateinit var bassBoost: BassBoost
    private lateinit var equalizer: Equalizer
    private lateinit var visualizer: Visualizer

    lateinit var searchHelper: SearchHelper
    private var currentMediaTrackData: MediaTrackData? = null
    private var prevMediaTrackData: MediaTrackData? = null
    private var nextMediaTrackData: MediaTrackData? = null


    private lateinit var controllerFuture: ListenableFuture<MediaController>
    lateinit var mediaController: MediaController
//    lateinit var playTracksManager: PlayTracksManager
//    private lateinit var tracksAsList: List<MediaTrackData>


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

    val trackListScrollPos: StateFlow<Int> =
        userPreferencesRepository.getInt(TRACK_LIST_SCROLL_POS)
            .stateIn(scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = runBlocking {
                    userPreferencesRepository.getInt(TRACK_LIST_SCROLL_POS).first()
                }
            )

    fun setTrackListScrollPos(trackListScrollPos: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setInt(TRACK_LIST_SCROLL_POS,trackListScrollPos)
        }
    }

    val playingItemId: StateFlow<Int> =
        userPreferencesRepository.getInt(PLAYING_ITEM_ID)
            .stateIn(scope = viewModelScope,
                started = SharingStarted.Eagerly,//WhileSubscribed(5_000),
                initialValue = runBlocking {
                    userPreferencesRepository.getInt(PLAYING_ITEM_ID).first()
                }
            )

    fun setPlayingItemId(itemId: Int?) {
        Log.d("SII","write $itemId")
        viewModelScope.launch {
            userPreferencesRepository.setInt(PLAYING_ITEM_ID,itemId)
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

    override fun onCreate(owner: LifecycleOwner) {//override lifecycle events
        super.onCreate(owner)
        Log.d("VMO","create")
    }
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("VMO","start")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("VMO","resume")

        if(::visualizer.isInitialized && ::mediaController.isInitialized){
            if(mediaController.isPlaying) {
                visualizer.setEnabled(true)
            }
        }

    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d("VMO","pause")
        super.onPause(owner)
        if(::visualizer.isInitialized) {
            visualizer.setEnabled(false)
        }

    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d("VMO","stop")
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d("VMO","destroy")
        super.onDestroy(owner)
    }

    fun expandArtistAlbumFor(mediaTrackData: MediaTrackData?){
        expandedArtists.add(mediaTrackData?.artist)
        expandedAlbums.add(mediaTrackData?.album)
    }

    fun <T>arrayToString(t: Array<T>):String{
        var rss=""
        for(i in 0..<t.size){
            rss+=t[i].toString()
            rss+= " "
        }
        return rss
    }

    fun updateMediaData(player: Player){
        _currentTrackMediaMetadata.value = player.mediaMetadata
        _sleevePicture.value = player.mediaMetadata.artworkData?.let {
            imageBitmapFromBytes(it)
        }
    }

    @OptIn(UnstableApi::class)
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    fun init(){

        if(::mediaController.isInitialized){
            return
        }

        _progressTitle.value = "Loading MediaSession"

        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            // MediaController is available here with controllerFuture.get()
            mediaController = controllerFuture.get()

            _isPlaying.value = mediaController.isPlaying
            if(mediaController.isPlaying){
                updateMediaData(mediaController)
            }

            val token = mediaController.connectedToken
            if(token != null && token.uid != 0) {
                visualizer = Visualizer(0)

                visualizer.setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener{
                        override fun onFftDataCapture(p0: Visualizer?, fft: ByteArray?, p2: Int) {
                            val n: Int? = fft?.size
                            if(n != null && n != 0) {
                                val magnitudes = FloatArray(n / 2 + 1)
                                val phases = FloatArray(n / 2 + 1)
                                magnitudes[0] = Math.abs(fft.get(0).toFloat()) // DC
                                magnitudes[n / 2] = Math.abs(fft.get(1).toFloat()) // Nyquist
                                phases[0] =
                                    0.also { phases[n / 2] = it.toFloat() }.toFloat()
                                for (k in 1..<n / 2) {
                                    val i = k * 2
                                    magnitudes[k] = hypot(fft.get(i).toDouble(), fft.get(i + 1).toDouble()).toFloat()
                                    phases[k] = atan2(fft.get(i + 1).toDouble(), fft.get(i).toDouble()).toFloat()
                                }
                                _magnitudes.value = magnitudes
//                                Log.d("VIS",arrayToString<Float>(magnitudes.toTypedArray()))
                            }

                        }

                        override fun onWaveFormDataCapture(
                            p0: Visualizer?,
                            p1: ByteArray?,
                            p2: Int
                        ) {
//                            TODO("Not yet implemented")
                        }
                    },10000,false,true
                )

                val cCaptureSize = visualizer.captureSize
                Log.d("VIS","$cCaptureSize")
            }

            visualizer.setCaptureSize(128)
            if(mediaController.isPlaying) {
                visualizer.setEnabled(true)
            }
            bassBoost = BassBoost(0,0)//sessionToken.uid)
            equalizer = Equalizer(0, 0)
//
            mediaController.addListener(
                object : Player.Listener {

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
//                        val token2 = mediaController?.connectedToken
//                        if(token2 != null && token2.uid != 0) {
//                            visualizer = Visualizer(0)
//                            val eq = Equalizer(0,token2.uid)
//                        }

                    }
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        super.onAudioSessionIdChanged(audioSessionId)

                        val eq = Equalizer(0,audioSessionId)

                    }
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
                        mediaController.seekToNext()
                        mediaController.prepare()
                        mediaController.play()
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
//                        _currentTrackMediaMetadata.value = mediaMetadata
                        Log.d("DMG", "mediaData changed")
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        _isPlaying.value = isPlaying

                        if(::visualizer.isInitialized) {
                            visualizer.setEnabled(isPlaying)
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
return

                        if(reason == 1){ //transition to next
                            prevMediaTrackData = currentMediaTrackData
                            currentMediaTrackData = nextMediaTrackData

                            if(searchHelper.overrunBottom){
                                searchHelper.overrunTop = true
                                searchHelper.overrunBottom = false
                            }else{
                                if(searchHelper.overrunTop){
                                    searchHelper.overrunTop = false
                                }
                            }

                            expandArtistAlbumFor(currentMediaTrackData)

                            setPlayingItemId(System.identityHashCode(currentMediaTrackData))
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

                                    if(searchHelper.overrunTop){
                                        searchHelper.overrunTop = false
                                        searchHelper.overrunBottom = true
                                    }else{
                                        if(searchHelper.overrunBottom)
                                        {
                                            searchHelper.overrunBottom = false
                                        }
                                    }

                                    expandArtistAlbumFor(currentMediaTrackData)

                                    setPlayingItemId(currentMediaTrackData?.getSystemId())

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

                                            if(searchHelper.overrunBottom){
                                                searchHelper.overrunTop = true
                                                searchHelper.overrunBottom = false
                                            }else{
                                                if(searchHelper.overrunTop){
                                                    searchHelper.overrunTop = false
                                                }
                                            }

                                            expandArtistAlbumFor(currentMediaTrackData)
                                            setPlayingItemId(currentMediaTrackData?.getSystemId())

                                            nextMediaTrackData = searchHelper.getNext(nextMediaTrackData)

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
                        Log.d("TRD","${searchHelper.overrunTop}, ${searchHelper.overrunBottom}")

                    }

//                    override fun onPlaybackStateChanged(playbackState: Int) {
//                        super.onPlaybackStateChanged(playbackState)
//                        if(playbackState == Player.STATE_READY){
//                            val md = mediaController.mediaMetadata()
//                        }
//                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
return
                        if(events.contains(Player.EVENT_AUDIO_SESSION_ID)){
                            val eq = Equalizer(0,0)//player.audioSessionId)

                        }

                        if(events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)){
                            if(player.playbackState == Player.STATE_READY){
                                updateMediaData(player)
                            }
                        }

                        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                            if (player.playbackState == Player.STATE_READY) {
                                updateMediaData(player)
                            }
                        }
                    }

                }
            )

            _mediaControllerLoaded.value = true
//            playTracksManager = PlayTracksManager(mediaController)
//            playTracksManager.searchHelper = searchHelper
            mediaController.repeatMode = playerRepeatMode.value
            _progressTitle.value = ""

        }, MoreExecutors.directExecutor())

//        viewModelScope.launch {
//            // Coroutine that will be canceled when the ViewModel is cleared.
//            while (true) {
//                reCheckMediaStoreGeneration()
//                delay(10000)
//            }
//        }


//        viewModelScope.launch {
//            playerRepeatMode.collect { newRepeatMode ->
//                if(::mediaController.isInitialized) {
//                    mediaController.repeatMode = newRepeatMode
//                }
//
//                if(this@MediaViewModel::searchHelper.isInitialized) {
//                    searchHelper.setRepeatMode(newRepeatMode)
//
//                    if(currentMediaTrackData != null) {
//
//                        if (newRepeatMode == Player.REPEAT_MODE_ALL && nextMediaTrackData == null) {  //current is last
//                            nextMediaTrackData = searchHelper.getNext(currentMediaTrackData)
//
//                            if (nextMediaTrackData != null) {
//                                val nextMediaItem = nextMediaTrackData?.uri?.let { uri -> MediaItem.fromUri(uri) }
//                                if (nextMediaItem != null) {
//                                    mediaController?.addMediaItem(nextMediaItem)
//
////                                    overrunBottom = true
//
////                                    val cn = mediaController?.mediaItemCount
////                                    if (cn == 4) {
////                                        mediaController?.removeMediaItem(0)
////                                    }
//                                }
//                            }
////                            else {
////                                mediaController?.removeMediaItem(0)
////                            }
//                        }
//
//                        if (newRepeatMode == Player.REPEAT_MODE_ALL && prevMediaTrackData == null) {  //current is first
//                            prevMediaTrackData = searchHelper.getPrev(currentMediaTrackData)
//
//                            if (prevMediaTrackData != null) {
//                                val prevMediaItem = prevMediaTrackData?.uri?.let { uri -> MediaItem.fromUri(uri) }
//                                if (prevMediaItem != null) {
//                                    mediaController?.addMediaItem(0, prevMediaItem)
//
////                                    overrunTop = true
//
////                                    val cn = mediaController?.mediaItemCount
////                                    if (cn == 4) {
////                                        mediaController?.removeMediaItem(0)
////                                    }
//                                }
//                            }
////                            else {
////                                mediaController?.removeMediaItem(0)
////                            }
//                        }
//
//                        if(newRepeatMode == Player.REPEAT_MODE_OFF){
//
//                            if(searchHelper.overrunTop){
//                                searchHelper.overrunTop = false
//                                mediaController?.removeMediaItem(0)
//                                prevMediaTrackData = null
//                            }
//
//                            if(searchHelper.overrunBottom){
//                                searchHelper.overrunBottom = false
//                                val cnt = mediaController?.mediaItemCount
//                                if(cnt != null) {
//                                    mediaController?.removeMediaItem(cnt - 1)
//                                    nextMediaTrackData = null
//                                }
//                            }
//
//                        }
//                    }
//
//                }
//            }
//        }

        viewModelScope.launch {
            querySortedResults.collect{
                searchHelper = SearchHelper(it)
                searchHelper.setRepeatMode(playerRepeatMode.value)
//                if(this@MediaViewModel::playTracksManager.isInitialized) {
//                    playTracksManager.searchHelper = searchHelper


//                }
            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("LCD","on Cleared")
    }

    fun play(){
        mediaController.prepare()
        mediaController.play()

    }

    fun pause(){
        mediaController.pause()
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



    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cursorToTrackList(cursor: Cursor): List<MediaTrackData>{
        val audioColumnIds = audioColumns.associateWith { cursor.getColumnIndex(it) }

        val list = (1 .. min(cursor.count,Int.MAX_VALUE)).map {
            cursor.moveToNext()
            cursorToMediaTrackData(cursor, audioColumnIds, context)
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

//        val mapByArtist = list.groupBy { it.artist }.toList().sortedBy { it.first }.toMap()
////        val mapByArtistEmpty = mapByArtist.entries.associate { it.key to mapOf(Pair("",listOf<MediaTrackData>())) }
//        val mapByArtistAlbumSortedByYear = mapByArtist.entries.associate{ it.key to it.value.groupBy { it1 -> it1.album }.toList().sortedBy { (key,value) -> value[0].year.toString() + key }.toMap() }
//        val mapByArtistAlbumSortedByYearTrack=mapByArtistAlbumSortedByYear.entries.associate { it.key to it.value.entries.associate {it1 -> it1.key to it1.value.sortedBy { mtd -> mtd.track } } }
//        _searchResults.value = mapByArtistAlbumSortedByYearTrack
////        _searchResults.value = mapByArtistEmpty

//        tracksAsList = mapByArtistAlbumSortedByYearTrack.flatMap { it.value.flatMap { it1 -> it1.value } }
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

        var cnt = 0

        try {
            val total = tracksList.value.count()
            for (i in 0..<tracksList.value.count()) {
                val it = tracksList.value[i]

                if (it.uri != null) {
                    Log.d("ADDT","${it.artist} ${it.title} $total")
                    val item = MediaItem.fromUri(it.uri)
                    mediaController.addMediaItem(item)
//                cnt =
                    cnt += 1

                    if (cnt > 40) {
                        break
                    }
                }
            }

//        tracksList.value.forEach{
//            if(it.uri != null) {
//                mediaController.addMediaItem(MediaItem.fromUri(it.uri))
//                cnt+=1
//
//                if(cnt > 1000){
//                    break
//                }
//            }
//        }

            mediaController.prepare()
//            mediaController.play()
        }catch (e:Exception){
            Log.d("EXC",e.toString())
//            mediaController.seekToNext()
//            mediaController.prepare()
//            mediaController.play()
        }
        return

        val sysId=mediaTrackData.getSystemId()
        Log.d("SII","at play: $sysId, title: ${mediaTrackData.title}")
        setPlayingItemId(sysId)
//        playTracksManager.setTrack(mediaTrackData)

        searchHelper.overrunTop = false
        searchHelper.overrunBottom = false

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
            play()
        }


    }

}