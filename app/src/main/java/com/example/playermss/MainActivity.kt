package com.example.playermss

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
import androidx.annotation.RequiresExtension
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
//import androidx.compose.material.icons.
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import com.example.playermss.data.MediaViewModel
import com.example.playermss.data.QueryParams
import com.example.playermss.data.SomeViewModel
import com.example.playermss.data.TextFieldViewModel
import com.example.playermss.ui.theme.PlayerMSSTheme
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

    private val searchOpen = SomeViewModel(true)
    
    private val searchFields = listOf(
        TextFieldViewModel("Artist"),
        TextFieldViewModel("Album"),
        TextFieldViewModel("Title"),
        TextFieldViewModel("FromYear"),
        TextFieldViewModel("ToYear"),
    )

    private var mediaViewModel = MediaViewModel()

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.applicationContext.also { mediaViewModel.context = it }
        mediaViewModel.init()
//        mediaViewModel.query(searchFields)

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

//        enableEdgeToEdge()
        setContent {
            PlayerMSSTheme {
                MainU(mediaViewModel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun MainU(mediaViewModel: MediaViewModel){
        ShowProgressOrContent(mediaViewModel.progressTitle.collectAsState().value) {
                Nav()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

    //todo: to coroutine
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
                                mediaViewModel.query(searchFields)

                            }

                            override fun onMediaScannerConnected() {
//                            TODO("Not yet implemented")
                                Log.d("DMS","Scanner connected")
                            }
                        })
                }
                Log.d("MVMR","after scan")
                mediaViewModel.query(searchFields)
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
//                        trackList?.clear()
                        mediaViewModel.mediaController?.clearMediaItems()
                        mediaViewModel.removeQuery(searchFields)
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
    fun StatusLine(){
        val generations = mediaViewModel.mediaStoreGenerations.collectAsState()
        Row{
            for(g in generations.value){
                Row{
                    Text(" $g")
                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun SearchScreen(onNav: () -> Unit){

        val isSearchOpen = searchOpen.state.collectAsState()
        val currentTrackMediaMetadata =
            mediaViewModel.currentTrackMediaMetadata.collectAsState()

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
//            bottomBar = {
//                BottomAppBar(
//                    actions = {
//                        IconButton(onClick = onNav) {
//                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
//                        }
//                        IconButton(onClick = { /* do something */ }) {
//                            Icon(
//                                Icons.Filled.Edit,
//                                contentDescription = "Localized description",
//                            )
//                        }
//                    },
//                    floatingActionButton = {
//                        FloatingActionButton(
//                            onClick = { /* do something */ },
//                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
//                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
//                        ) {
////                            Icon(Icons.Filled.Add, "Localized description")
//                            MinimalDropdownMenu()
//                        }
//                    }
//                )
//            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                Column {
                    Row {
                        StatusLine()
                    }
                    Row {
                        if (isSearchOpen.value != null && isSearchOpen.value == true) {
                            SearchFields(searchFields, mediaViewModel)
                        }
                    }
                    Row(Modifier.weight(7f)) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f)) {
                                SleevePicture(mediaViewModel.sleevePicture.collectAsState().value)
                            }
                            Row(Modifier.weight(1f)) {
                                ShowQueryResults(mediaViewModel)
                            }

//                            Row(horizontalArrangement = Arrangement.Center,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .wrapContentSize()
////                                    .weight(1f + weightAdd)
////                            .animateContentSize()
////                                    .clickable { //Log.d("D_CLICK", "Box Sleeve clicked!")
////                                        weightAdd = if (weightAdd < 1f) {
////                                            4f
////                                        } else {
////                                            0f
////                                        }
////                                    }
//                            ) {
//                                SleevePicture(mediaController = mediaViewModel.mediaController)
//                            }
//                            Row(modifier = Modifier
////                                .weight(7f - weightAdd)
////                        .animateContentSize()
//                                .clickable {
//                                    Log.d("D_CLICK", "track list clicked!")
//                                    weightAdd = 0f
//                                }
//                            )
//                            {
//                                ShowSearchResults(mediaViewModel)
//                            }

                        }
                    }

                    Row (Modifier.padding(top=4.dp)){
                        Box {//for overlap
                            Row {
                                TrackTime(mediaViewModel.mediaController)
                            }
                            Row {
                                TrackInfo(
                                    mediaMetadata = currentTrackMediaMetadata.value,
                                    Modifier
                                        .padding(start = 14.dp, top = 4.dp)
                                        .width(intrinsicSize = IntrinsicSize.Max)
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                        .weight(4f)
                                )
                                Text("", Modifier.weight(1.2f))
                            }
                        }
                    }

                    Row {
                        Column(Modifier.weight(4f)) {
                            PlayControls(
                                mediaController = mediaViewModel.mediaController,
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
                                IconButton(onClick = {
                                    isSearchOpen.value?.let {
                                        searchOpen.set(
                                            !it
                                        )
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search in Library"
                                    )
                                }

                                MinimalDropdownMenu()
                            }
                        }
                    }
                }
            }

        }

    }



    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun Nav() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = NavSearch) {
            composable<NavSearch> { SearchScreen(onNav={navController.navigate(route = NavTrackList)}) }
            composable<NavTrackList> { TracksScreen(onNav={navController.navigate(route = NavSearch)}) }
        }
    }






    @RequiresApi(Build.VERSION_CODES.Q)
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
                        MinimalDropdownMenu()
                    }
                }
            )
        },
        ) { innerPadding ->

//            ShowPlayList(mediaViewModel.cursor)
        }
    }



















    @Deprecated("obsolete, by architecture changes")
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
                        it.value?.let { it1 -> TrackCardFromCursor(it1,mediaViewModel.mediaController) }
                    }
                }
            }
        }

    }



    enum class UI_State{
        TrackList,
        Search
    }

    @Deprecated("by MVMV")
    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun UI_TrackList(queryParams: QueryParams?, cb:(UI_State)->Unit){

        var cursor:Cursor? = null// by remember { mutableStateOf<Cursor?>(null) }
        var tracksUpdated by remember { mutableStateOf(false) }

//        val titleColumnInd = remember { cursor.value?.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE) }

        LaunchedEffect(queryParams) {

            Log.d("DQP","Query resolver")

            val f1 = MediaStore.getExternalVolumeNames(applicationContext)
            val contentUri = MediaStore.Audio.Media.getContentUri(f1.elementAt(0))

            cursor = contentResolver.query(
            contentUri,
            queryParams?.projection,
            queryParams?.selection,
            queryParams?.selectionArgs,
            queryParams?.sortOrder
            )

            val idColumnInd = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumnInd = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor?.moveToNext() == true) {

                val data: String? = dataColumnInd?.let { it1 -> cursor?.getString(it1) }
                data?.let { Log.d("DTA", it)}
                val id: Long? = idColumnInd?.let { it1 -> cursor?.getLong(it1) }
                val cUri = id?.let { it1 -> ContentUris.withAppendedId(contentUri, it1) }
                val mediaItem = cUri?.let { it1 -> MediaItem.fromUri(it1) }
                if (mediaItem != null) {
                    mediaViewModel.mediaController?.addMediaItem(mediaItem)
                }
            }

            if(cursor?.count!! > 0){
                mediaViewModel.mediaController?.prepare()
                mediaViewModel.mediaController?.play()
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
                    TrackCardFromCursor(it,mediaViewModel.mediaController)
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
        val mediaControllerLoaded = mediaViewModel.mediaControllerLoaded.collectAsState()
        val currentTrackMediaMetadata = mediaViewModel.currentTrackMediaMetadata.collectAsState()

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
//                    SleevePicture(mediaController = mediaViewModel.mediaController)
                }
                Box {
                    Row {
                        TrackTime(mediaViewModel.mediaController)
                    }
                    Row {
                        TrackInfo(
                            mediaMetadata = currentTrackMediaMetadata.value,
                            Modifier
                                .padding(start = 14.dp, top = 4.dp)
                                .width(intrinsicSize = IntrinsicSize.Max)
                                .basicMarquee(iterations = 50)
                                .weight(4f)
                        )
                        Text("", Modifier.weight(1.2f))
                    }
                }

                Row {
                    Column(Modifier.weight(4f)) {
                        PlayControls(
                            mediaController = mediaViewModel.mediaController,
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


    //////////////to rem
    @kotlin.OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainUI() {
        val mediaControllerLoaded = mediaViewModel.mediaControllerLoaded.collectAsState()
        val currentTrackMediaMetadata = mediaViewModel.currentTrackMediaMetadata.collectAsState()

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
//                        if (trackList != null) {
//                            trackList!!.asList()
//                        } else {
//                            Text("TrackList is empty")
//                        }
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
//                    SleevePicture(mediaController = mediaViewModel.mediaController)
                }
                Box {
                    Row {
                        TrackTime(mediaViewModel.mediaController)
                    }
                    Row {
                        TrackInfo(
                            mediaMetadata = currentTrackMediaMetadata.value,
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
                            mediaController = mediaViewModel.mediaController,
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
