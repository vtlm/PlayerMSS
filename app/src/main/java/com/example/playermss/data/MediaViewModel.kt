package com.example.playermss.data

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import com.example.playermss.PlayerMSSReleaseApplication
import com.example.playermss.R
import com.example.playermss.data.searchhelper.SearchHelper
import com.example.playermss.data.searchhelper.SearchHelperAlbumTrack
import com.example.playermss.data.searchhelper.SearchHelperArtistAlbumTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

//val TRACK_LIST_SCROLL_POS = intPreferencesKey("track_list_scroll_pos")
val IS_REMAIN_TIME = booleanPreferencesKey("is_remain_time")
val PLAYER_REPEAT_MODE = intPreferencesKey("player_repeat_mode")
val USER_TRACK_LIST_SCROLL_POS = intPreferencesKey("user_track_list_scroll_pos")
val PLAYING_ITEM_ID = intPreferencesKey("playing_item_id")
//val IS_UI_SEARCH_VISIBLE = booleanPreferencesKey("is_ui_search_visible")
val IS_VISUALIZER_VISIBLE = booleanPreferencesKey("is_visualizer_visible")
val TRACK_LIST_SORT_MODE = intPreferencesKey("track_list_sort_mode")

data class MediaScannerErrorEntry(
    var filePath: String?
)

data class MediaScannerEntry(
    var filePath: String?,
    var contentUri :Uri?
)

data class MediaScannerResults(
    var added: Int = 0,
    var errors: Int = 0,
    var errorsDescr: MutableList<MediaScannerErrorEntry> = mutableListOf(),
    var entriesDescr: MutableList<MediaScannerEntry> = mutableListOf()
)


@HiltViewModel
class MediaViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDataRepository: UserDataRepository,
    @ApplicationContext val context: Context
): ViewModel(), DefaultLifecycleObserver {

//    private lateinit var controllerFuture: ListenableFuture<MediaController>
    lateinit var mediaController: MediaController

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressTitle = MutableStateFlow("")
    val progressTitle: StateFlow<String> = _progressTitle.asStateFlow()
//    fun setProgressTitle(title: String){_progressTitle.value = title}

//    private val _mediaStoreGenerations = MutableStateFlow(listOf<Long>())
//    val mediaStoreGenerations: StateFlow<List<Long>> = _mediaStoreGenerations.asStateFlow()

    private val _scanResults = MutableStateFlow(mutableListOf<String>())
    val scanResults: StateFlow<MutableList<String>> = _scanResults.asStateFlow()

    private val _mediaScanResults = MutableStateFlow(MediaScannerResults())
    val mediaScanResults: StateFlow<MediaScannerResults> = _mediaScanResults.asStateFlow()

    private val _currentTrackMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentTrackMediaMetadata: StateFlow<MediaMetadata?> = _currentTrackMediaMetadata.asStateFlow()

    private val tracksList: StateFlow<List<MediaTrackData>> = userDataRepository.asStateFlow(viewModelScope)

    private val _querySortedResults = MutableStateFlow<List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>?>(null)//(sortByArtistAlbumAsPairs(tracksList.value))
    val querySortedResults: StateFlow<List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>?> = _querySortedResults.asStateFlow()

    private val _queryGroupedByAlbumCD = MutableStateFlow<List<Pair<Pair<String, String>, List<MediaTrackData>>>?>(null)
    val queryGroupedByAlbumCD: StateFlow<List<Pair<Pair<String, String>, List<MediaTrackData>>>?> = _queryGroupedByAlbumCD.asStateFlow()

    private var _magnitudes = MutableStateFlow(floatArrayOf())
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    val expandedArtists = StringSetDataStorePreferences("expandedArtists", userPreferencesRepository, viewModelScope)
    val expandedAlbums = StringSetDataStorePreferences("expandedAlbums", userPreferencesRepository, viewModelScope)

    lateinit var lazyListState: LazyListState

    private val userScrollPos = userPreferencesRepository.getOrDefault(USER_TRACK_LIST_SCROLL_POS, 0)
    fun setUserScrollPos(pos: Int) = userPreferencesRepository.set(USER_TRACK_LIST_SCROLL_POS, viewModelScope, pos)

    val trackListSortModeOrd = userPreferencesRepository.getOrDefaultAsStateFlow(TRACK_LIST_SORT_MODE, viewModelScope, TrackSortMode.ArtistAlbumTrack.ordinal)
    fun setTrackListSortMode(trackSortMode: TrackSortMode) = userPreferencesRepository.set(
        TRACK_LIST_SORT_MODE, viewModelScope, trackSortMode.ordinal)

    private val _scrollPos = MutableStateFlow(0)
    val scrollPos: StateFlow<Int?> = _scrollPos.asStateFlow()




//    private val _currentTime = MutableStateFlow(0L)
//    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

//    private val _fullTime = MutableStateFlow(0L)
//    val fullTime: StateFlow<Long> = _fullTime.asStateFlow()

//    private val _showTime = MutableStateFlow("")
//    val showTime: StateFlow<String> = _showTime.asStateFlow()

//    var _userInteraction = false
//    fun setUserInteraction(userInteraction: Boolean){
//        _userInteraction = userInteraction
//    }
//    fun onSliderChange(value: Float){
//        setUserInteraction(true)
//        _currentTime.value = (value * 1000).toLong()
//    }
//    fun onSliderChangeFinished(){
//        setUserInteraction(false)
//        mediaController.seekTo(_currentTime.value)
//    }


//    private fun <T>getFirstFromFlow(t: Flow<T>):T?{
//        var r: T? = null
//        viewModelScope.launch {
//            r = t.first()
//        }
//        return r
//    }

    fun getCurrentTrackLazyListIndex(): Int?{
        if(::lazyListState.isInitialized){
            val item = lazyListState.layoutInfo.visibleItemsInfo.find { it.key == playingItemId.value}
            return item?.index
        }
        return null
    }

    val queryFields = arrayOf(
        QueryTextField(context.resources.getString(R.string.Artist), userPreferencesRepository, viewModelScope),
        QueryTextField(context.resources.getString(R.string.Album), userPreferencesRepository, viewModelScope),
        QueryTextField(context.resources.getString(R.string.Title), userPreferencesRepository, viewModelScope),
        QueryTextField(context.resources.getString(R.string.From_Year), userPreferencesRepository, viewModelScope),
        QueryTextField(context.resources.getString(R.string.To_Year), userPreferencesRepository, viewModelScope),
        QueryTextField(context.resources.getString(R.string.File_System_Path), userPreferencesRepository, viewModelScope),
    )

//    private var _sleevePicture = MutableStateFlow<ImageBitmap?>(null)
//    val sleevePicture: StateFlow<ImageBitmap?> = _sleevePicture.asStateFlow()

//    private lateinit var bassBoost: BassBoost
//    private lateinit var equalizer: Equalizer
    private lateinit var visualizer: Visualizer

    lateinit var searchHelper: SearchHelper//ArtistAlbumTrack
    private var currentMediaTrackData: MediaTrackData? = null
    private var prevMediaTrackData: MediaTrackData? = null
    private var nextMediaTrackData: MediaTrackData? = null


    private fun setTrackListScrollPos(trackListScrollPos: Int) {
        //Log.d("DBGL","VM: traCkListScrollPos: $trackListScrollPos")
        _scrollPos.value = trackListScrollPos
    }

//    var _isRemainTime = false
    val isRemainTime: StateFlow<Boolean> = userPreferencesRepository.getOrDefaultAsStateFlow(IS_REMAIN_TIME, viewModelScope, false)
    fun setRemainTime(isRemainTime: Boolean) = userPreferencesRepository.set(IS_REMAIN_TIME, viewModelScope ,isRemainTime)
//    fun toggleRemainTime(){
//        setRemainTime( !_isRemainTime )
//    }

    val playingItemId: StateFlow<Int> = userPreferencesRepository.getOrDefaultAsStateFlow(PLAYING_ITEM_ID, viewModelScope, 0)
    fun setPlayingItemId(itemId: Int?) = userPreferencesRepository.set(PLAYING_ITEM_ID,viewModelScope, itemId)

    val playerRepeatMode: StateFlow<Int> = userPreferencesRepository.getOrDefaultAsStateFlow(PLAYER_REPEAT_MODE, viewModelScope, 0)
    private fun setPlayerRepeatMode(playerRepeatMode: Int) = userPreferencesRepository.set(PLAYER_REPEAT_MODE,viewModelScope, playerRepeatMode)

    fun incPlayerRepeatMode() {
        var nextPlayerRepeatMode = playerRepeatMode.value + 1
        if(nextPlayerRepeatMode > Player.REPEAT_MODE_ALL){
            nextPlayerRepeatMode = Player.REPEAT_MODE_OFF
        }
        setPlayerRepeatMode(nextPlayerRepeatMode)
    }

//    val isSearchVisible = userPreferencesRepository.getOrDefaultAsStateFlow(IS_UI_SEARCH_VISIBLE, viewModelScope,true)
//    fun setSearchVisible(state: Boolean) = userPreferencesRepository.set(IS_UI_SEARCH_VISIBLE, viewModelScope, state)

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()
    fun setSearchVisible(value: Boolean){
        _isSearchVisible.value = value
    }

    val isVisualizerVisible = userPreferencesRepository.getOrDefaultAsStateFlow(IS_VISUALIZER_VISIBLE, viewModelScope,false)
//    fun setVisualizerVisible(state: Boolean) = userPreferencesRepository.set(IS_VISUALIZER_VISIBLE, viewModelScope, state)

    override fun onCreate(owner: LifecycleOwner) {//override lifecycle events
        super.onCreate(owner)
        //Log.d("VMO","create")
    }
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        //Log.d("VMO","start")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        //Log.d("VMO","resume")

        if(::visualizer.isInitialized && ::mediaController.isInitialized){
            if(mediaController.isPlaying) {
                visualizer.setEnabled(true)
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        //Log.d("VMO","pause")
        super.onPause(owner)
        if(::visualizer.isInitialized) {
            visualizer.setEnabled(false)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        //Log.d("VMO","stop")
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        //Log.d("VMO","destroy")
        super.onDestroy(owner)
    }

    fun expandArtistAlbumFor(mediaTrackData: MediaTrackData?){
        expandedArtists.add(mediaTrackData?.artist)
        expandedAlbums.add(mediaTrackData?.relativePath)
    }

//    fun <T>arrayToString(t: Array<T>):String{
//        var rss=""
//        for(i in 0..<t.size){
//            rss+=t[i].toString()
//            rss+= " "
//        }
//        return rss
//    }

    fun isSearchFieldsEmpty(): Boolean{
        return queryFields.find { it.text.value != "" } == null
    }

    fun updateMediaData(player: Player){
        _currentTrackMediaMetadata.value = player.mediaMetadata
//        _sleevePicture.value = player.mediaMetadata.artworkData?.let {
//            imageBitmapFromBytes(it)
//        }
    }

private val playerListener = object: Player.Listener {

//        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
//            super.onPlayWhenReadyChanged(playWhenReady, reason)
////                        val token2 = mediaController?.connectedToken
////                        if(token2 != null && token2.uid != 0) {
////                            visualizer = Visualizer(0)
////                            val eq = Equalizer(0,token2.uid)
////                        }
//
//        }

        //                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
//                        super.onAudioSessionIdChanged(audioSessionId)
//
//                        val eq = Equalizer(0,audioSessionId)
//
//                    }
        override fun onPlayerError(error: PlaybackException) {
//            val cause = error.cause
//            if (cause is HttpDataSource.HttpDataSourceException) {
//                // An HTTP error occurred.
//                val httpError = cause
//                // It's possible to find out more about the error both by casting and by querying
//                // the cause.
//                if (httpError is HttpDataSource.InvalidResponseCodeException) {
//                    // Cast to InvalidResponseCodeException and retrieve the response code, message
//                    // and headers.
//                } else {
//                    // Try calling httpError.getCause() to retrieve the underlying cause, although
//                    // note that it may be null.
//                }
//            }
//            mediaController.seekToNext()
//            mediaController.prepare()
//            mediaController.play()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
//                        _currentTrackMediaMetadata.value = mediaMetadata
            //Log.d("DMG", "mediaData changed")
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

            //Log.d("DLM","hello from listener $this r: $reason")

            if(reason == 1){ //transition to next
                prevMediaTrackData = currentMediaTrackData
                currentMediaTrackData = nextMediaTrackData

                checkTrackListScrollDown()
                expandArtistAlbumFor(currentMediaTrackData)
                setPlayingItemId(currentMediaTrackData?.getHash())

                nextMediaTrackData = searchHelper.getNext(nextMediaTrackData)
                val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }
                if (nextMediaItem != null) {
                    mediaController.removeMediaItem(0)
                    mediaController.addMediaItem(nextMediaItem)
                }
            }

            if(reason == 2){ //seek to prev, next buttons

                if(mediaController.mediaItemCount > 0) {
                    val firstMediaItem = mediaController.getMediaItemAt(0)
//prev
                    if (mediaItem == firstMediaItem) {
                        nextMediaTrackData = currentMediaTrackData
                        currentMediaTrackData = prevMediaTrackData

                        checkTrackListScrollUp()
                        expandArtistAlbumFor(currentMediaTrackData)
                        setPlayingItemId(currentMediaTrackData?.getHash())

                        prevMediaTrackData = searchHelper.getPrev(prevMediaTrackData)
                        val prevMediaItem = prevMediaTrackData?.uri?.let { MediaItem.fromUri(it) }

                        if (prevMediaItem != null) {
                            mediaController.addMediaItem(0, prevMediaItem)

                            val cn = mediaController.mediaItemCount
                            if (cn == 4) {
                                mediaController.removeMediaItem(3)
                            }
                        }else{
                            val cn = mediaController.mediaItemCount
                            if(cn != 0) {
                                mediaController.removeMediaItem(cn - 1)
                            }
                        }

                    }else{
//next?
                        val lastIemIndex = mediaController.mediaItemCount - if (mediaController.mediaItemCount > 0) 1 else 0
                        val lastMediaItem = mediaController.getMediaItemAt(lastIemIndex)

                        if (mediaItem == lastMediaItem) {
//next
                            if (nextMediaTrackData != null) {

                                prevMediaTrackData = currentMediaTrackData
                                currentMediaTrackData = nextMediaTrackData

                                checkTrackListScrollDown()
                                expandArtistAlbumFor(currentMediaTrackData)
                                setPlayingItemId(currentMediaTrackData?.getHash())

                                nextMediaTrackData = searchHelper.getNext(nextMediaTrackData)

                                if (nextMediaTrackData != null) {
                                    val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }

                                    if (nextMediaItem != null) {
                                        mediaController.addMediaItem(nextMediaItem)

                                        val cn = mediaController.mediaItemCount
                                        if (cn == 4) {
                                            mediaController.removeMediaItem(0)
                                        }
                                    }
                                }else{
                                    mediaController.removeMediaItem(0)
                                }
                            }
                        }
                    }
                }
            }
            //Log.d("TRD","${searchHelper.overrunTop}, ${searchHelper.overrunBottom}")
        }

//                    override fun onPlaybackStateChanged(playbackState: Int) {
//                        super.onPlaybackStateChanged(playbackState)
//                        if(playbackState == Player.STATE_READY){
//                            val md = mediaController.mediaMetadata()
//                        }
//                    }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

//                        if(events.contains(Player.EVENT_AUDIO_SESSION_ID)){
//                            val eq = Equalizer(0,0)//player.audioSessionId)
//
//                        }

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

    private fun init(){
        _isPlaying.value = mediaController.isPlaying
        if (mediaController.isPlaying) {
            updateMediaData(mediaController)
        }

//            val token = mediaController.connectedToken
//            if(token != null && token.uid != 0) {
//
//                visualizer = Visualizer(0)
//
//                visualizer.setDataCaptureListener(
//                    object : Visualizer.OnDataCaptureListener{
//                        override fun onFftDataCapture(p0: Visualizer?, fft: ByteArray?, p2: Int) {
//                            val n: Int? = fft?.size
//                            if(n != null && n != 0) {
//                                val magnitudes = FloatArray(n / 2 + 1)
//                                val phases = FloatArray(n / 2 + 1)
//                                magnitudes[0] = Math.abs(fft.get(0).toFloat()) // DC
//                                magnitudes[n / 2] = Math.abs(fft.get(1).toFloat()) // Nyquist
//                                phases[0] =
//                                    0.also { phases[n / 2] = it.toFloat() }.toFloat()
//                                for (k in 1..<n / 2) {
//                                    val i = k * 2
//                                    magnitudes[k] = hypot(fft.get(i).toDouble(), fft.get(i + 1).toDouble()).toFloat()
//                                    phases[k] = atan2(fft.get(i + 1).toDouble(), fft.get(i).toDouble()).toFloat()
//                                }
//                                _magnitudes.value = magnitudes
////                                //Log.d("VIS",arrayToString<Float>(magnitudes.toTypedArray()))
//                            }
//
//                        }
//
//                        override fun onWaveFormDataCapture(
//                            p0: Visualizer?,
//                            p1: ByteArray?,
//                            p2: Int
//                        ) {
//                        }
//                    },10000,false,true
//                )
//
//                val cCaptureSize = visualizer.captureSize
//                //Log.d("VIS","$cCaptureSize")
//                visualizer.setCaptureSize(128)
//                if(mediaController.isPlaying) {
//                    visualizer.setEnabled(true)
//                }
//                bassBoost = BassBoost(0,0)//sessionToken.uid)
//                equalizer = Equalizer(0, 0)
////
//            }
//
        //Log.d("DLM", "Mediacontroller: $mediaController")
        PlayerMSSReleaseApplication.setPlayerListener(playerListener)

        mediaController.repeatMode = playerRepeatMode.value


        viewModelScope.launch {
            _scrollPos.value = userScrollPos.first()
        }

// experimental for trackTime
//
//        viewModelScope.launch(Dispatchers.Default) {
//            isRemainTime.collect{
//                _isRemainTime = it
//                Timber.d("$_isRemainTime")
//            }
//        }


//        viewModelScope.launch {
//            playingItemId.collect{ it ->
//                while (!::searchHelper.isInitialized){
//                    delay(100)
//                }
//            }
//        }

        viewModelScope.launch {
            playerRepeatMode.collect { newRepeatMode ->
                if (::mediaController.isInitialized) {
                    mediaController.repeatMode = newRepeatMode
                }

                if (this@MediaViewModel::searchHelper.isInitialized) {
                    searchHelper.repeatMode = newRepeatMode

                    if (currentMediaTrackData != null) {

                        if (newRepeatMode == Player.REPEAT_MODE_ALL && nextMediaTrackData == null) {  //current is last
                            nextMediaTrackData = searchHelper.getNext(currentMediaTrackData)

                            nextMediaTrackData?.let {
                                it.uri?.let { uri ->
                                    MediaItem.fromUri(uri).let { mediaItem ->
                                        mediaController.addMediaItem(mediaItem)
                                    }
                                }
                            }
                        }

                        if (newRepeatMode == Player.REPEAT_MODE_ALL && prevMediaTrackData == null) {  //current is first
                            prevMediaTrackData = searchHelper.getPrev(currentMediaTrackData)

                            prevMediaTrackData?.let {
                                it.uri?.let { uri ->
                                    MediaItem.fromUri(uri).let { mediaItem ->
                                        mediaController.addMediaItem(0, mediaItem)
                                    }
                                }
                            }
                        }

                        if (newRepeatMode == Player.REPEAT_MODE_OFF) {

                            if (searchHelper.overrunTop) {
                                searchHelper.overrunTop = false
                                mediaController.removeMediaItem(0)
                                prevMediaTrackData = null
                            }

                            if (searchHelper.overrunBottom) {
                                searchHelper.overrunBottom = false
                                val cnt = mediaController.mediaItemCount
                                if (cnt != 0) {
                                    mediaController.removeMediaItem(cnt - 1)
                                    nextMediaTrackData = null
                                }
                            }

                        }
                    }

                }
            }
        }

//        viewModelScope.launch(Dispatchers.Default) {
//            querySortedResults.collect {
//                searchHelper = SearchHelperArtistAlbumTrack(it)
//                searchHelper.repeatMode = playerRepeatMode.value
//
//                currentMediaTrackData = searchHelper.getMediaTrackDataForCode(playingItemId.value)
//                prevMediaTrackData = searchHelper.getPrev(currentMediaTrackData)
//                nextMediaTrackData = searchHelper.getNext(currentMediaTrackData)
//
//                if (it?.size == 1) {
//                    expandedArtists.add(it[0].first)
//                }
//            }
//        }

        viewModelScope.launch(Dispatchers.Default) {
            trackListSortModeOrd.collect{
                groupAndSortTracks(tracksList.value, toSortMode(it))
            }
        }

// experimental for trackTime
//

//        viewModelScope.launch {
//            while (true) {
//                if (!_userInteraction && mediaController.isPlaying) {
//                    mediaController.contentDuration.let { _fullTime.value = it }
//                    mediaController.currentPosition.let { _currentTime.value = it }
//                }
//                val calcTime = if (isRemainTime.value) _fullTime.value - _currentTime.value else _currentTime.value
//                val time = "%1\$tM:%1\$tS".format(calcTime)
//                _showTime.value = if (isRemainTime.value) "-$time" else time
//
//                delay(1000)
//            }
//        }

    }


    //    @OptIn(UnstableApi::class)
//    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    init {
        _progressTitle.value = context.resources.getString(R.string.Loading_MediaSession)
        viewModelScope.launch {
            while(!PlayerMSSReleaseApplication.isMediaControllerInitialized()){
                delay(200)
            }
            mediaController = PlayerMSSReleaseApplication.mediaController
            init()
            _progressTitle.value = ""
        }
    }//init

    fun checkTrackListScrollUp() {
        if (searchHelper.checkOverrunsFromTopToBottom()) {
            //Log.d("DBGL", "Overrun T B")
            with(lazyListState) {
                val topFromEndItemIndex = layoutInfo.totalItemsCount - layoutInfo.visibleItemsInfo.size
                setTrackListScrollPos(topFromEndItemIndex)
            }
        } else {
            scrollUp()
        }
    }

    fun checkTrackListScrollDown(){
        if(searchHelper.checkOverrunsFromBottomToTop()){
            //Log.d("DBGL", "Overrun B T")
            with(lazyListState){
                setTrackListScrollPos(0)
            }
        }else{
            scrollDown()
        }
    }

    override fun onCleared() {
        super.onCleared()
        //Log.d("LCD","on Cleared")
    }

    private fun scrollDown() {
        val currentTrackIndex = getCurrentTrackLazyListIndex()
        with(lazyListState) {
            if (currentTrackIndex != null
                && currentTrackIndex > firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size / 2
                && currentTrackIndex < firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size
            ) {
                var newFirstVisibleItemIndex = firstVisibleItemIndex + 1
                val topFromEndItemIndex = layoutInfo.totalItemsCount - layoutInfo.visibleItemsInfo.size
                if(newFirstVisibleItemIndex > topFromEndItemIndex){
                    newFirstVisibleItemIndex = topFromEndItemIndex
                }
                //Log.d("DBGL", "ScrollDown to $newFirstVisibleItemIndex")

                setTrackListScrollPos(newFirstVisibleItemIndex)
            }
        }
    }

    private fun scrollUp(){
        val currentTrackIndex = getCurrentTrackLazyListIndex()
        with(lazyListState) {
            if (currentTrackIndex != null
                && currentTrackIndex > firstVisibleItemIndex
                && currentTrackIndex < firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size / 2
            ) {
                var newFirstVisibleItemIndex = firstVisibleItemIndex - 1
                if(newFirstVisibleItemIndex < 0){
                    newFirstVisibleItemIndex = 0
                }
                //Log.d("DBGL", "Scrollup to $newFirstVisibleItemIndex")

                setTrackListScrollPos(newFirstVisibleItemIndex)
            }
        }
    }

    fun play(){
        if(mediaController.mediaItemCount == 0){
            currentMediaTrackData = searchHelper.getMediaTrackDataForCode(playingItemId.value)
            currentMediaTrackData?.let { play(it) }
        }else {
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun pause(){
        mediaController.pause()
    }

    fun seekToPrevious(){
        if(mediaController.mediaItemCount == 0){
            currentMediaTrackData = searchHelper.getMediaTrackDataForCode(playingItemId.value)
            prevMediaTrackData = searchHelper.getPrev(currentMediaTrackData)
            prevMediaTrackData?.let { play(it) }
        }else {
            mediaController.seekToPrevious()
        }
    }

    fun seekToNext(){
        if(mediaController.mediaItemCount == 0){
            currentMediaTrackData = searchHelper.getMediaTrackDataForCode(playingItemId.value)
            nextMediaTrackData = searchHelper.getNext(currentMediaTrackData)
            nextMediaTrackData?.let { play(it) }
        }else {
            mediaController.seekToNext()
        }
    }

//    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun reCheckMediaStoreGeneration(){
//        val externalVolumeNames = MediaStore.getExternalVolumeNames(context)
//        val generations = externalVolumeNames.map{MediaStore.getGeneration(context,it)}
//
//        if(_mediaStoreGenerations.value != generations){
//            _mediaStoreGenerations.value = generations
//        }
//
//    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cursorToTrackList(cursor: Cursor): List<MediaTrackData>{
        val audioColumnIds = audioColumns.associateWith { cursor.getColumnIndex(it) }

        val list = (1 .. min(cursor.count,Int.MAX_VALUE)).map {
            cursor.moveToNext()
            cursorToMediaTrackData(cursor, audioColumnIds, context)
        }

        return list.filterNotNull()
    }

    private fun groupByPathAsAlbumCD(listIn: List<MediaTrackData>):  List<Pair<Pair<String, String>, List<MediaTrackData>>>{
        val mapByPathAlbumCD = listIn.groupBy { Pair(it.relativePath, it.album) }
        val sortedList = mapByPathAlbumCD.toList().sortedWith(compareBy({it.second[0].artist}, {it.second[0].year}, {it.first.second}))
        return sortedList
    }

    private fun sortByArtistAlbumAsPairs(list: List<MediaTrackData>):  List<Pair<String, List<Pair<String, List<MediaTrackData>>>>>{
        val setByArtist = list.groupBy { it.artist }.toList().toSortedSet(compareBy { it.first })
        val listByArtistAlbum = setByArtist.map { Pair(it.first, it.second.groupBy { it1 -> it1.relativePath }.toList().toSortedSet(
            compareBy { it2 -> it2.second[0].year.toString() + it2.first }
        )) }
        val listByArtistAlbumTrack = listByArtistAlbum.map { Pair(it.first, it.second.map { it2 -> Pair(it2.first, it2.second.sortedBy{ mtd -> mtd.track})}) }
        return listByArtistAlbumTrack
    }

    private fun groupAndSortTracks(list: List<MediaTrackData>, sortMode: TrackSortMode){
        when(sortMode){
            TrackSortMode.ArtistAlbumTrack -> {
                val sortedList = sortByArtistAlbumAsPairs(list)
                _querySortedResults.value = sortedList
                searchHelper = SearchHelperArtistAlbumTrack(sortedList)

                if (sortedList.size == 1) {
                    expandedArtists.add(sortedList[0].first)
                }

            }
            TrackSortMode.AlbumTrack -> {
                val sortedList = groupByPathAsAlbumCD(list)
                _queryGroupedByAlbumCD.value = sortedList
                searchHelper = SearchHelperAlbumTrack(sortedList)
            }
            TrackSortMode.Path -> {}
        }
        setUserScrollPos(0)
        searchHelper.repeatMode = playerRepeatMode.value
        currentMediaTrackData = searchHelper.getMediaTrackDataForCode(playingItemId.value)
        prevMediaTrackData = searchHelper.getPrev(currentMediaTrackData)
        nextMediaTrackData = searchHelper.getNext(currentMediaTrackData)

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateQueryStatistic(list: List<MediaTrackData>){

        viewModelScope.launch(Dispatchers.Default) {
            userDataRepository.saveTrackList(list)
        }

        groupAndSortTracks(list, toSortMode(trackListSortModeOrd.value))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams(), contentUri: Uri): Cursor?{

        val cursor: Cursor? = this@MediaViewModel.context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs?.toTypedArray(),
                queryParams?.sortOrder
            )
//            //Log.d("MVM", "Items: ${cursor?.count}")
//        }
        return cursor
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(){

        mediaController.stop()
//        _sleevePicture.value = null
        _currentTrackMediaMetadata.value = null
        setSearchVisible(false)


        viewModelScope.launch(Dispatchers.Default) {

            _progressTitle.value = context.resources.getString(R.string.Querying_MediaStore)
            queryFields.forEach { it.saveText() }

            val queryParams = QueryParams()
            queryParams.projection = audioColumns

            queryParams.selection = "${MediaStore.Audio.Media.ARTIST} like ?" +
                    " and ${MediaStore.Audio.Media.ALBUM} like ?" +
                    " and ${MediaStore.Audio.Media.TITLE} like ?" +
                    " and ${MediaStore.Audio.Media.DATA} like ?" +
                    " and ${MediaStore.Audio.Media.DATA} not like '%.wma%'"

            queryParams.selectionArgs = mutableListOf(
                "%${queryFields[0].text.value}%",
                "%${queryFields[1].text.value}%",
                "%${queryFields[2].text.value}%",
                "%${queryFields[5].text.value}%",
            )

            val fromYear = queryFields[3].text.value.toIntOrNull()
            fromYear?.let {
                queryParams.selection += " and ${MediaStore.Audio.Media.YEAR} >= CAST(? as integer)"
                queryParams.selectionArgs += it.toString()
            }

            queryFields[4].text.value.toIntOrNull()?.let {
                queryParams.selection += " and ${MediaStore.Audio.Media.YEAR} <= CAST(? as integer)"
                queryParams.selectionArgs += it.toString()
            }

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
//            setUserScrollPos(0)
            _scrollPos.value = 0
            _progressTitle.value = ""
        }
    }


    fun play(mediaTrackData: MediaTrackData){

        val sysId=mediaTrackData.getHash()
        //Log.d("SII","at play: $sysId, title: ${mediaTrackData.title}")
        setPlayingItemId(sysId)

        searchHelper.clearOverruns()

        //Log.d("DTP", mediaTrackData.title)
        mediaController.clearMediaItems()

        var offset = 0

        currentMediaTrackData = mediaTrackData

        prevMediaTrackData = searchHelper.getPrev(mediaTrackData)
        val prevMediaItem = prevMediaTrackData?.uri?.let { MediaItem.fromUri(it) }

        prevMediaItem?.let {
            mediaController. addMediaItem(0, prevMediaItem)
            offset = 1
        }

        val mediaItem = mediaTrackData.uri?.let { MediaItem.fromUri(it) }

        mediaItem?.let { mediaController.addMediaItem(it) }

        nextMediaTrackData = searchHelper.getNext(mediaTrackData)
        val nextMediaItem = nextMediaTrackData?.uri?.let { MediaItem.fromUri(it) }

        nextMediaItem?.let { mediaController.addMediaItem(it) }

        val count = mediaController.mediaItemCount
        if (count > 0) {
            mediaController.seekTo(offset,0)
            play()
        }
    }

    val supportedAudioTypes = arrayOf("audio/mpeg")

    val localScanResults = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun runScanFilesInDir(directoryUri: Uri, application: Application){
        viewModelScope.launch(Dispatchers.Default) {
            _progressTitle.value = "Scanning FileSystem"
            localScanResults.clear()
            _scanResults.value.clear()
            scanFilesInDir(directoryUri, application)
            _scanResults.value += localScanResults.sorted()
            _progressTitle.value = ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun scanFilesInDir(directoryUri: Uri, application: Application){
        val documentsTree = DocumentFile.fromTreeUri(application, directoryUri)
        val paths = documentsTree?.listFiles()
//        val filesToScan: MutableList<String> = mutableListOf()
        if (paths != null) {
            for(path in paths){
                if(path.isDirectory){
                    scanFilesInDir(path.uri, application)
                }else{
                    val type = application.contentResolver.getType(path.uri)
                    //Log.d("SFD","type: $type ${path.uri}")
//                    val typeInfo = contentResolver.getTypeInfo(path.uri)
                    if(supportedAudioTypes.contains(type)){
                        DocumentFileCompat.fromUri(context,path.uri)
                            ?.getAbsolutePath(context)?.let {
                                localScanResults += it
                            }
                    }
                }
            }
        }
    }

    fun runMediaScanner(){
        _progressTitle.value = "MediaScanner running"
        viewModelScope.launch (Dispatchers.Default){
            val mediaScannerResults = MediaScannerResults()
            val entriesCnt = _scanResults.value.size
            MediaScannerConnection.scanFile(context, _scanResults.value.toTypedArray(),
                arrayOf("audio/mpeg","audio/mp3","*/*"),
                object: MediaScannerConnection.MediaScannerConnectionClient {
                    override fun onScanCompleted(path: String?, uri: Uri?) {
                        //Log.d("DMS","Scan completed: uri: $uri, path $path")
                        if(uri != null){
                            mediaScannerResults.added += 1
                            mediaScannerResults.entriesDescr += MediaScannerEntry(path, uri)
                        }else{
                            mediaScannerResults.errors += 1
                            mediaScannerResults.errorsDescr += MediaScannerErrorEntry(path)
                        }
                        if(mediaScannerResults.errors + mediaScannerResults.added == entriesCnt) {
                            _mediaScanResults.value = mediaScannerResults
                            _progressTitle.value = ""
                        }
                    }
                    override fun onMediaScannerConnected() {
                        //Log.d("DMS","Scanner connected")
                    }
                })

//            _mediaScanResults.value = mediaScannerResults

        }
    }


}