package com.example.playermss

//import androidx.compose.material.icons.
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import com.example.compose.AppTheme
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

//todo
//rotation (face state, remember)
//permissions on startup

@Serializable
object NavSearch
@Serializable
object NavTrackList

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val mediaViewModel:MediaViewModel by viewModels()

    var _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted = _permissionsGranted.asStateFlow()

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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
                _permissionsGranted.value = true
                permissions.entries.forEach {
                    Log.i("DEBUG", "${it.key} = ${it.value}")
                    if (it.value) {
                        println("Successful......")

                    }else{
                        _permissionsGranted.value = false
                    }
                }

                if(_permissionsGranted.value){
                    this.applicationContext.also { mediaViewModel.context = it }
                    mediaViewModel.init()
                    lifecycle.addObserver(mediaViewModel)
                }
            }

            val requiredPermissions: MutableList<String> = mutableListOf()

            checkIsPermissionGranted(android.Manifest.permission.RECORD_AUDIO, requiredPermissions)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                checkIsPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE, requiredPermissions)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkIsPermissionGranted(android.Manifest.permission.READ_MEDIA_AUDIO, requiredPermissions)
            }

            requestPermissionLauncher.launch(requiredPermissions.toTypedArray())


//        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainU(permissionsGranted.collectAsState().value)
            }
        }
    }

    fun checkIsPermissionGranted(permission: String, collector: MutableList<String>){
        if(ContextCompat.checkSelfPermission(applicationContext, permission) == -1){
            collector += permission
        }
    }

    fun triggerRestart(context: Activity) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        if (context is Activity) {
            (context as Activity).finish()
        }
        Runtime.getRuntime().exit(0)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun MainU(permissionsGranted: Boolean){
        when(permissionsGranted) {
         false -> {
             Box(
                 contentAlignment = Alignment.Center,
                 modifier = Modifier.fillMaxSize()
                     .background(color = MaterialTheme.colorScheme.background)
             ) {
                 Text("wait for permissions")
             }
         }
         true -> {
                ShowProgressOrContent(mediaViewModel.progressTitle.collectAsState().value) {
                    Nav()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

//    //todo check it
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
////        findViewById<ConstraintLayout>(R.id.main).invalidate();
//
//        // Checks whether a keyboard is available
//        if (newConfig.keyboardHidden === Configuration.KEYBOARDHIDDEN_YES) {
//            Toast.makeText(this, "Keyboard available", Toast.LENGTH_SHORT).show()
//        } else if (newConfig.keyboardHidden === Configuration.KEYBOARDHIDDEN_NO) {
//            Toast.makeText(this, "No keyboard", Toast.LENGTH_SHORT).show()
//        }
//    }

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
//                                mediaViewModel.query()

                            }

                            override fun onMediaScannerConnected() {
//                            TODO("Not yet implemented")
                                Log.d("DMS","Scanner connected")
                            }
                        })
                }
                Log.d("MVMR","after scan")
//                mediaViewModel.query()
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
//                        mediaViewModel.removeQuery(searchFields)
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

    @Composable
    fun ShowUnsortedTracks(tracks: List<MediaTrackData>){
        LazyColumn {
            tracks.forEach{
                item {
                    Text(it.title)
                }
            }
        }
    }

    @Composable
    fun ShowMagnitude(magnitudes: FloatArray){
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val cnt = magnitudes.size

            if (cnt > 0) {

                val step = canvasWidth/cnt
                val max = magnitudes.maxOrNull()
                if (max != null) {
                    val mult = max / canvasHeight

                    if (mult > 0f) {
                        for (i in 0..<magnitudes.size) {

                            val barHeight = magnitudes[i] / mult

                            drawRect(
                                Color.Yellow,
                                topLeft = Offset(x = i * step.toFloat(), y = canvasHeight - barHeight),
                                size = Size(step - 1f, barHeight)
                            )
//                            drawLine(
//                                start = Offset(x = i.toFloat() * 2, y = 0f),
//                                end = Offset(x = i.toFloat() * 2, y = magnitudes[i] * mult),
//                                color = Color.Yellow
//                            )
                        }
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun SearchScreen(onNav: () -> Unit){

        val currentTrackMediaMetadata = mediaViewModel.currentTrackMediaMetadata.collectAsState()
        val isSearchOpen = mediaViewModel.isSearchVisible.collectAsState()

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
//                    Row {
//                        StatusLine()
//                    }
                    Row {
                        if (isSearchOpen.value == true) {
                            SearchFields(mediaViewModel.queryFields, mediaViewModel::query)
                        }
                    }
//                    Row(Modifier.weight(1f)){
//                        ShowUnsortedTracks(mediaViewModel.tracksList.collectAsState().value)
//                    }
                    Row(Modifier.weight(1f)){
                        ShowMagnitude(mediaViewModel.magnitudes.collectAsState().value)
                    }
                    Row(Modifier.weight(7f)) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f)) {
                                SleevePicture(mediaViewModel.sleevePicture.collectAsState().value)
                            }
                            Row(Modifier.weight(1f)) {
                                ShowQueryResults(
                                    mediaViewModel.querySortedResults.collectAsState().value,
                                    mediaViewModel.expandedArtists.externalStringSet.collectAsState().value,
                                    mediaViewModel.expandedAlbums.externalStringSet.collectAsState().value,
//                                    mediaViewModel.trackListScrollPos.collectAsState().value,
                                    mediaViewModel.scrollPos.collectAsState().value,
                                    mediaViewModel
                                )
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
                                TrackTime(mediaViewModel)
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
                                mediaViewModel.isPlaying.collectAsState().value,
                                mediaViewModel.playerRepeatMode.collectAsState().value,
//                                mediaController = mediaViewModel.mediaController,
                                mediaViewModel,
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
                                    isSearchOpen.value.let {
                                        mediaViewModel.setSearchVisible(
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


}