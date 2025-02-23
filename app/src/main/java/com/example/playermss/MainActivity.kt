package com.example.playermss

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.media.audiofx.BassBoost
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
//import androidx.compose.material.icons.
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel
import com.example.playermss.data.QueryParams
import com.example.playermss.data.TextFieldViewModel
import com.example.playermss.ui.theme.PlayerMSSTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

//todo
//rotation (face state, remember)
//permissions on startup


@Serializable
object NavSearch

@Serializable
object NavTrackList



@UnstableApi
class MainActivity : ComponentActivity() {

    private var trackList: TrackList? = null
    private var mediaControllerLoaded = mutableStateOf(false)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var cMediaMetadata = mutableStateOf<MediaMetadata?>(null)


    private val searchFields = listOf<TextFieldViewModel>(
        TextFieldViewModel("Artist"),
        TextFieldViewModel("Album"),
        TextFieldViewModel("Title"),
        TextFieldViewModel("Year"),
    )

    private var mediaViewModel = MediaViewModel()

        private var bassBoost: BassBoost? = null
    private  var visualizer: Visualizer? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.applicationContext.also { mediaViewModel.context = it }
        mediaViewModel.query(queryParams = QueryParams())

// Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher. You can use either a val, as shown in this snippet,
// or a lateinit var in your onAttach() or onCreate() method.

//        val requestPermissionLauncher1 =
//            registerForActivityResult(
//                ActivityResultContracts.RequestPermission()
//            ) { isGranted: Boolean ->
//                if (isGranted) {
//                    // Permission is granted. Continue the action or workflow in your
//                    // app.
//                } else {
//                    // Explain to the user that the feature is unavailable because the
//                    // feature requires a permission that the user has denied. At the
//                    // same time, respect the user's decision. Don't link to system
//                    // settings in an effort to convince the user to change their
//                    // decision.
//                }
//            }
//        requestPermissionLauncher1.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)


// Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher. You can use either a val, as shown in this snippet,
// or a lateinit var in your onAttach() or onCreate() method.
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.entries.forEach {
                    Log.i("DEBUG", "${it.key} = ${it.value}")
                    if (it.value) {
                        println("Successful......")

                    }
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }

        val r= ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.READ_MEDIA_AUDIO)
        val r1= ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.RECORD_AUDIO)
        val r2= ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.READ_EXTERNAL_STORAGE)




        val sessionToken =
            SessionToken(
                applicationContext,
                ComponentName(applicationContext, PlaybackService::class.java)
            )
        controllerFuture =
            MediaController.Builder(applicationContext, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // MediaController is available here with controllerFuture.get()
            mediaController = controllerFuture?.get()

            if (trackList == null) {
                trackList = TrackList(applicationContext, mediaController!!)
            }

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
                        cMediaMetadata.value = mediaMetadata
                    }
                }
            )

            mediaControllerLoaded.value = true

        }, MoreExecutors.directExecutor())

//        val browserFuture = MediaBrowser.Builder(applicationContext, sessionToken).buildAsync()
//        browserFuture.addListener({
//            // MediaBrowser is available here with browserFuture.get()
//            // Get the library root to start browsing the library tree.
//            val mediaBrowser = browserFuture.get()
//            val rootFuture = mediaBrowser.getLibraryRoot(/* params= */ null)
//            rootFuture.addListener({
//                // Root node MediaItem is available here with rootFuture.get().value
//                val root = rootFuture.get()
//            }, MoreExecutors.directExecutor())
//
//        }, MoreExecutors.directExecutor())

//        enableEdgeToEdge()
        setContent {
            PlayerMSSTheme {
//                UI_Main2()
                Column {
                    Nav()
                }
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
//        if(mediaController?.isPlaying == true){
//        mediaController?.stop()
//        mediaController?.clearMediaItems()
//        }
        mediaController?.release()

    }
//todo check it
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        findViewById<ConstraintLayout>(R.id.main).invalidate();

        // Checks whether a keyboard is available
        if (newConfig.keyboardHidden === Configuration.KEYBOARDHIDDEN_YES) {
            Toast.makeText(this, "Keyboard available", Toast.LENGTH_SHORT).show()
        } else if (newConfig.keyboardHidden === Configuration.KEYBOARDHIDDEN_NO) {
            Toast.makeText(this, "No keyboard", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val addTracks =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == RESULT_OK){
                result.data?.data?.also { directoryUri ->
                    // Perform operations on the document using its URI.
                    Log.d("DBG", directoryUri.toString())
                    val documentsTree = DocumentFile.fromTreeUri(application, directoryUri)
                    val paths = documentsTree?.listFiles()
                        //todo: add media extension
                        ?.filter { it.uri.toString().endsWith("mp3") }
                        ?.map{DocumentFileCompat.fromUri(applicationContext,it.uri)}
                        ?.map{it?.getAbsolutePath(applicationContext)}
                        ?.toTypedArray()

                    MediaScannerConnection.scanFile(applicationContext,paths,
                        arrayOf("audio/mp3","*/*"),
                        object: MediaScannerConnectionClient {
                            override fun onScanCompleted(path: String?, uri: Uri?) {
//                            TODO("Not yet implemented")
                                Log.d("DMS","Scan completed: uri: $uri, path $path")
                                Log.d("MVMR","after scan")
                                mediaViewModel.query()

                            }

                            override fun onMediaScannerConnected() {
//                            TODO("Not yet implemented")
                                Log.d("DMS","Scanner connected")
                            }
                        })
                }
                Log.d("MVMR","after scan")
                mediaViewModel.query()
            }
        }

    @Composable
    fun ShowPlayList(cursor: StateFlow<Cursor?>){
        val _cursor=cursor.collectAsState()
        Log.d("SHOW","update ${_cursor.value?.count}")
        Text("${_cursor.value?.count}")

        _cursor.value?.moveToFirst()

        LazyColumn {
            _cursor.let {
                it.value?.count?.let { it1 ->
                    items(it1){ itemInd->
                        it.value?.moveToPosition(itemInd)
                        it.value?.let { it1 -> TrackCardFromCursor(it1,mediaController) }
                    }
                }
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun MinimalDropdownMenu() {
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .padding(2.dp)
        ) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },

                ) {
                DropdownMenuItem(
                    text = { Text("Clear") },
                    onClick = {
                        trackList?.clear()
                        mediaController?.clearMediaItems()
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add dir") },
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
                        }
                        addTracks.launch(intent)

                        expanded = false
                    }
                )
            }
        }
    }

    @Composable
    fun SearchField(ind:Int, textValue: StateFlow<String>){
        val text = textValue.collectAsState()

        OutlinedTextField(
            value = text.value,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {str -> searchFields[ind].setText(str)},
            label = { Text(searchFields[ind].description) },
            isError = false,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { mediaViewModel.query(searchFields)}
            ))

    }

    @Composable
    fun SearchFields(){
        LazyColumn {
            items(searchFields.count()){
                SearchField(it, searchFields[it].textField)
            }
        }
    }


    @Composable
    fun SearchResults(cursor_: StateFlow<Cursor?>){
        val cursor = cursor_.collectAsState()
    }


    private fun LazyListScope.showTracks(tracks: List<MediaTrackData>){
        tracks.forEach(){
            item {
                Text("        ${it.title}")
            }
        }
    }

    private fun LazyListScope.showAlbums(albums: Map<String, Pair<List<MediaTrackData>, Boolean>>){
            albums.forEach(){
                item {
                    Text("    ${it.key}")
                }
                if(it.value.second) {
                    showTracks(it.value.first)
                }
            }
    }

    private fun LazyListScope.showArtist(artist: Map. Entry<String, Pair<Map<String, Pair<List<MediaTrackData>, Boolean>>, Boolean>>){
        item {
            Card(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                    .padding(2.dp)

                    .clickable(
                        onClick = {

                            mediaViewModel.toggleArtistExpanded(artist.key)
//                            mediaController?.seekTo(mediaData.listIndex, 0)
//                            if (mediaController?.isPlaying != true) {
//                                mediaController?.prepare()
//                                mediaController?.play()
//                            }
//                            onClick()
                        }),
                shape = RoundedCornerShape(10),
//                colors = if(mediaData.listIndex == playingIndex) CardDefaults.elevatedCardColors() else CardDefaults.cardColors()
            ) {
                Text("Artist: ${artist.key}")
            }
        }

        if(artist.value.second) {
            showAlbums(artist.value.first)
        }
    }


    @Composable
    fun ShowSearchResults(resultsSF: StateFlow<Map<String, Pair<Map<String, Pair<List<MediaTrackData>, Boolean>>, Boolean>>>, updateCounter: StateFlow<Int>){
        val results = resultsSF.collectAsState()
        val counter = updateCounter.collectAsState()

        Log.d("FL","Counter: ${counter.value}")

        LazyColumn {
            results.value.forEach(){
                showArtist(it)
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun SearchScreen(onNav: () -> Unit){
        Scaffold (
            bottomBar = {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = onNav) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Localized description",
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
//                            Icon(Icons.Filled.Add, "Localized description")
                            MinimalDropdownMenu()
                        }
                    }
                )
            },
        ) { innerPadding ->

            Column {
                SearchFields()


//                LazyColumn(modifier = Modifier.fillMaxSize()) {
//                    item {
//                        Text(text = "Outer Item 1", modifier = Modifier.padding(8.dp))
//                    }
//                    item {
//                        LazyColumn(modifier = Modifier.fillParentMaxSize()) {
//                            items(50) { innerIndex ->
//                                Text(
//                                    text = "Inner Item #$innerIndex",
//                                    modifier = Modifier.padding(8.dp)
//                                )
//                            }
//                        }
//                    }
//                }


//                LazyColumn() {
//                    items(5){
//                        Column {
//                            Text("$it")
//
//                            LazyColumn(modifier = Modifier.fillParentMaxSize()) {
//                                items(5) { it1 ->
//                                    Text("$it $it1")
//                                }
//                            }
//                        }
//                    }
//                }


                ShowSearchResults(mediaViewModel.searchResults, mediaViewModel.updateCounter)

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TracksScreen(onNav: () -> Unit){
        Scaffold (
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = onNav) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Localized description",
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* do something */ },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
//                        Icon(Icons.Filled.Add, "Localized description")
                        MinimalDropdownMenu()
                    }
                }
            )
        },
        ) { innerPadding ->

            ShowPlayList(mediaViewModel.cursor)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun Nav() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = NavSearch) {
            composable<NavSearch> { SearchScreen(onNav={navController.navigate(route = NavTrackList)}) }
            composable<NavTrackList> { TracksScreen(onNav={navController.navigate(route = NavSearch)}) }
        }
    }




    enum class UI_State{
        TrackList,
        Search
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun UI_TrackList(queryParams: QueryParams?, cb:(UI_State)->Unit){

        var cursor:Cursor? = null// by remember { mutableStateOf<Cursor?>(null) }
        var tracksUpdated by remember { mutableStateOf(false) }

//        val titleColumnInd = remember { cursor.value?.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE) }

        LaunchedEffect(queryParams) {

            Log.d("DQP","Query resolver")

            val f1= MediaStore.getExternalVolumeNames(applicationContext)
            val contentUri= MediaStore.Audio.Media.getContentUri(f1.elementAt(0))

            cursor = contentResolver.query(
            contentUri,
            queryParams?.projection,
            queryParams?.selection,
            queryParams?.selectionArgs,
            queryParams?.sortOrder
            )

            val idColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val idData = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor?.moveToNext() == true) {
                val data: String? = idData?.let { it1 -> cursor?.getString(it1) }
                data?.let { Log.d("DTA", it)}
                val id: Long? = idColumn?.let { it1 -> cursor?.getLong(it1) }
                val cUri = id?.let { it1 -> ContentUris.withAppendedId(contentUri, it1) }
                val mediaItem = cUri?.let { it1 -> MediaItem.fromUri(it1) }
                if (mediaItem != null) {
                    mediaController?.addMediaItem(mediaItem)
                }
            }

            if(cursor?.count!! > 0){
                mediaController?.prepare()
                mediaController?.play()
            }
        }

        DisposableEffect(queryParams) {
            onDispose {
                cursor?.close()
            }
        }

        cursor?.moveToFirst()

        LazyColumn {
            cursor?.let {
                items(it.count){itemInd->
                    it.moveToPosition(itemInd)
                    TrackCardFromCursor(it,mediaController)
                }
            }
        }

    }

    @Composable
    fun UI_Search(cb:(UI_State)->Unit){

    }

    @kotlin.OptIn(ExperimentalFoundationApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun UI_Main2(){

        var uiState = remember { mutableStateOf(UI_State.TrackList) }
        var queryParams = remember { mutableStateOf<QueryParams?>(null) }

        fun setUI_State(ui_state: UI_State){
            uiState.value = ui_state
        }

        var weightAdd by remember {
            mutableFloatStateOf(0f)
        }

        val offs: Float by animateFloatAsState(
            targetValue = weightAdd,
            // Configure the animation duration and easing.
            animationSpec = tween(durationMillis = 800, easing = EaseInOutExpo),
            label = "offs"
        )

        Scaffold(
            modifier = Modifier.fillMaxSize()//.padding(top = 36.dp)
        ) { innerPadding ->
            Column {
                Row(modifier = Modifier
                    .weight(7f - offs)
                    .animateContentSize()
                    .clickable {
                        Log.d("D_CLICK", "track list clicked!")
                        weightAdd = 0f
                    }
                ) {
                    if (mediaControllerLoaded.value) {
                        when (uiState.value){
                            UI_State.TrackList -> UI_TrackList(queryParams.value,::setUI_State)
                            UI_State.Search -> UI_Search(::setUI_State)
                        }
                    } else {
                        Text("loading mediaSession")
                    }
                }
                Row(modifier = Modifier
                    .weight(2f + offs)
                    .animateContentSize()
                    .clickable { //Log.d("D_CLICK", "Box Sleeve clicked!")
                        if (weightAdd < 1f) {
                            weightAdd = 4f
                        } else {
                            weightAdd = 0f
                        }
                    }
                ) {
                    SleevePicture(mediaController = mediaController)
                }
                Box {
                    Row {
                        TrackTime(mediaController)
                    }
                    Row {
                        TrackInfo(
                            mediaMetadata = cMediaMetadata.value,
                            Modifier
                                .padding(start = 14.dp, top = 4.dp)
                                .width(intrinsicSize = IntrinsicSize.Max)
                                .basicMarquee()
                                .weight(4f)
                        )
                        Text("", Modifier.weight(1.2f))
                    }
                }

                Row {
                    Column(Modifier.weight(4f)) {
                        PlayControls(
                            mediaController = mediaController,
                            modifier = Modifier.weight(4f)
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
//                        .width(intrinsicSize = IntrinsicSize.Max)
                            .fillMaxWidth()
                            .background(color = Color.Magenta)
                    ) {
                        Row(//verticalAlignment = Alignment.CenterVertically,
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            MinimalDropdownMenu()
                        }
                    }
                }
            }
        }


    }



    ////////////////////////////////////////////to trm
    @SuppressLint("RememberReturnType")
    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun LibraryMainUI(){


//        fun setCursor(cursorP: Cursor){
//            cursor.value = cursorP
//        }
        if (mediaControllerLoaded.value) {
//            SearchUI(mediaController as Player, ::setCursor)
//            UI_TrackList() { }
        } else {
            Text("loading mediaSession")
        }



    }


    //////////////to rem
    @kotlin.OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainUI() {

        var weightAdd by remember {
            mutableFloatStateOf(0f)
        }

        val offs: Float by animateFloatAsState(
            targetValue = weightAdd,
            // Configure the animation duration and easing.
            animationSpec = tween(durationMillis = 800, easing = EaseInOutExpo),
            label = "offs"
        )

        Scaffold(
            modifier = Modifier.fillMaxSize()//.padding(top = 36.dp)
        ) { innerPadding ->
            Column {
                Row(modifier = Modifier
                    .weight(7f - offs)
                    .animateContentSize()
                    .clickable {
                        Log.d("D_CLICK", "track list clicked!")
                        weightAdd = 0f
                    }
                ) {
                    if (mediaControllerLoaded.value) {
                        if (trackList != null) {
                            trackList!!.asList()
                        } else {
                            Text("TrackList is empty")
                        }
                    } else {
                        Text("loading mediaSession")
                    }
                }
                Row(modifier = Modifier
                    .weight(2f + offs)
                    .animateContentSize()
                    .clickable { //Log.d("D_CLICK", "Box Sleeve clicked!")
                        if (weightAdd < 1f) {
                            weightAdd = 4f
                        } else {
                            weightAdd = 0f
                        }
                    }
                ) {
                    SleevePicture(mediaController = mediaController)
                }
                Box {
                    Row {
                        TrackTime(mediaController)
                    }
                    Row {
                        TrackInfo(
                            mediaMetadata = cMediaMetadata.value,
                            Modifier
                                .padding(start = 14.dp, top = 4.dp)
                                .width(intrinsicSize = IntrinsicSize.Max)
                                .basicMarquee()
                                .weight(4f)
                        )
                        Text("", Modifier.weight(1.2f))
                    }
                }

                Row {
                    Column(Modifier.weight(4f)) {
                        PlayControls(
                            mediaController = mediaController,
                            modifier = Modifier.weight(4f)
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
//                        .width(intrinsicSize = IntrinsicSize.Max)
                            .fillMaxWidth()
                            .background(color = Color.Magenta)
                    ) {
                        Row(//verticalAlignment = Alignment.CenterVertically,
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            MinimalDropdownMenu()
                        }
                    }
                }
            }
        }
    }

}
