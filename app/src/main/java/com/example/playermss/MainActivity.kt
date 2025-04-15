package com.example.playermss

//import androidx.compose.material.icons.
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.example.playermss.data.MediaScannerResults
import com.example.playermss.data.MediaTrackData
import com.example.playermss.data.MediaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlin.system.exitProcess

//todo
//rotation (face state, remember)
//permissions on startup

@Serializable
object NavSearch
@Serializable
object NavTrackList
@Serializable
object NavScannedResults
@Serializable
object NavMediaScannerResults

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val mediaViewModel:MediaViewModel by viewModels()

//    private var _permissionsChecking = MutableStateFlow(true)
//    private val permissionsChecking = _permissionsChecking.asStateFlow()

    private var _statusString = MutableStateFlow("")
    private val statusString = _statusString.asStateFlow()

    private var _permissionsGranted = MutableStateFlow(false)
    private val permissionsGranted = _permissionsGranted.asStateFlow()

    private var isReCheckPermissions = false

    private val neededPermissions:  MutableList<String> = mutableListOf("")

    var pendingFunCall: () -> Unit = {}

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 1)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        mediaViewModel.setUserScrollPos(0)
        checkPermissions()

//        enableEdgeToEdge()
        setContent {
            AppTheme {
//                Text("hey")
                MainU(permissionsGranted.collectAsState().value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("DPR","onResume")
        if(isReCheckPermissions){
            isReCheckPermissions = false
            checkPermissions()
        }
    }

    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher. You can use either a val, as shown in this snippet,
// or a lateinit var in your onAttach() or onCreate() method.
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            neededPermissions.clear()
            _permissionsGranted.value = true
            permissions.entries.forEach {
                Log.i("DEBUG", "${it.key} = ${it.value}")
                if (it.value) {
                    println("Successful......")

                }else{
                    neededPermissions += it.key
                    _permissionsGranted.value = false
                }
            }

            if(_permissionsGranted.value){
//                mediaViewModel.setUserScrollPos(0)
                lifecycle.addObserver(mediaViewModel)
            }

            _statusString.value = ""
        }


    private fun checkPermissions(){

        _statusString.value = resources.getString(R.string.checking_permissions)

        val requiredPermissions: MutableList<String> = mutableListOf()

        checkIsPermissionGranted(android.Manifest.permission.RECORD_AUDIO, requiredPermissions)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkIsPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE, requiredPermissions)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkIsPermissionGranted(android.Manifest.permission.READ_MEDIA_AUDIO, requiredPermissions)
        }

        requestPermissionLauncher.launch(requiredPermissions.toTypedArray())

    }

    private fun checkIsPermissionGranted(permission: String, collector: MutableList<String>){
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
        ShowProgressOrContent(statusString.collectAsState().value) {
            when(permissionsGranted) {
             false -> {
                 RequestPermissionsScreen()
             }
             true -> {
                      Nav()
                }
            }
        }
    }

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun RequestPermissionsScreen(){
    Column {
        Row(Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {

                Text(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    text = resources.getString(R.string.wait_for_permissions)
                )
            }
        }
        Row(Modifier.weight(1f)){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                Column {
                    neededPermissions.forEachIndexed{ ind, str ->
                        val permissionInfo = applicationContext.packageManager.getPermissionInfo(str,PackageManager.GET_META_DATA)
                        val description = permissionInfo.loadDescription(applicationContext.packageManager)
                        Text(text = "${ind + 1}. ${description.toString()}",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                    }
                }
            }

        }
        Row(Modifier.weight(1f)){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {

                Column(Modifier.fillMaxWidth(),
//                    Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally){
                    Button(onClick = {
                        isReCheckPermissions = true
//                                checkPermissions()
                        reCheckPermissions.launch(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", packageName, null)
                        })
                    },
                        Modifier.fillMaxWidth(0.9f)
                            .padding(vertical = 4.dp)) {
                        Text(text = resources.getString(R.string.Go_To_Settings))
                    }
                    Button(onClick = {
//                                finishAffinity();
                        finishAndRemoveTask()
                        exitProcess(0);
                    },
                    modifier = Modifier.padding(vertical = 4.dp))
                    { Text(text = resources.getString(R.string.Exit))}
                }
            }

        }
    }

}

    override fun onDestroy() {
        super.onDestroy()
    }

    //todo: to coroutine
    @RequiresApi(Build.VERSION_CODES.Q)
    private val reCheckPermissions =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("DPR","in afterPermissions $result")
//            checkPermissions()
//            triggerRestart(this)
            if(result.resultCode == RESULT_OK){
                result.data?.data?.also {
                    Log.d("DPR","afterPermissions")
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val addTracks =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            mediaViewModel.scanResults.value.clear()
            if(result.resultCode == RESULT_OK){
                result.data?.data?.also { directoryUri ->
                    // Perform operations on the document using its URI.
                    Log.d("DBG", directoryUri.toString())
                    mediaViewModel.runScanFilesInDir(directoryUri, application)
                    pendingFunCall()
                }
            }
        }



    @RequiresApi(Build.VERSION_CODES.Q)
    @Composable
    fun MinimalDropdownMenu(onNav: () -> Unit) {
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
//                DropdownMenuItem(
//                    text = { Text("Clear") },
//                    onClick = {
////                        trackList?.clear()
//                        mediaViewModel.mediaController.clearMediaItems()
////                        mediaViewModel.removeQuery(searchFields)
//                        expanded = false
//                    }
//                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.Add_Dir)) },
                    onClick = {
                        addTracks.launch(Intent(
                            Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .apply { putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")}
                        )
                        expanded = false
                        pendingFunCall = onNav
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
        ShowProgressOrContent(mediaViewModel.progressTitle.collectAsState().value) {

            val querySortedResults = mediaViewModel.querySortedResults.collectAsState().value
            val currentTrackMediaMetadata = mediaViewModel.currentTrackMediaMetadata.collectAsState().value
            val isSearchOpen = if (querySortedResults.isEmpty()) true
                                else
                                    mediaViewModel.isSearchVisible.collectAsState().value
            val isVisualizerVisible = mediaViewModel.isVisualizerVisible.collectAsState().value

            Scaffold()
             { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    Column {
                        Row {
                            if (isSearchOpen) {
                                SearchFields(mediaViewModel.queryFields, mediaViewModel::query)
                            }
                        }
                        if (isVisualizerVisible) {
                            Row(Modifier.weight(1f)) {
                                ShowMagnitude(mediaViewModel.magnitudes.collectAsState().value)
                            }
                        }
                        Row(Modifier.weight(7f)) {
                                if(querySortedResults.isNotEmpty()) {
                                    Column(Modifier.fillMaxSize()) {
                                        Row(Modifier.weight(1f)) {
                                            SleevePicture(mediaViewModel.currentTrackMediaMetadata.collectAsState().value)
                                        }
                                        Row(Modifier.weight(1f)) {

                                            ShowQueryResults(
                                                querySortedResults,
                                                mediaViewModel.expandedArtists.externalStringSet.collectAsState().value,
                                                mediaViewModel.expandedAlbums.externalStringSet.collectAsState().value,
//                                    mediaViewModel.trackListScrollPos.collectAsState().value,
                                                mediaViewModel.scrollPos.collectAsState().value,
                                                mediaViewModel
                                            )
                                        }
                                    }
                                }else{
//                                            Box(
//                                                contentAlignment = Alignment.Center,
//                                                modifier = Modifier.fillMaxSize()
//                                            ) {

                                                Column(Modifier.fillMaxSize()) {
                                                    if(mediaViewModel.isSearchFieldsEmpty()){
                                                        Row(Modifier.fillMaxWidth(0.9f).weight(2f),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically){
                                                            Text(text = stringResource(R.string.Use_empty_search_fields_to_find_all_in_MediaStore_can_take_a_time),
                                                                textAlign = TextAlign.Center)
                                                        }
                                                    }
                                                    Row(modifier = Modifier.fillMaxSize().weight(1f),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically) {
                                                        Button(onClick = { mediaViewModel.query() }) {
                                                            Text(stringResource(R.string.Find))
                                                        }
                                                    }
                                                }
                                }
                        }


                        Row(Modifier.padding(top = 4.dp)) {
                            Box {//for overlap
                                Row {
                                    TrackTime(
                                        mediaViewModel.mediaController,
                                        mediaViewModel.isRemainTime.collectAsState().value,
                                        mediaViewModel::setRemainTime
                                    )
                                }
                                Row {
                                    TrackInfo(
                                        mediaMetadata = currentTrackMediaMetadata,
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
//                                .background(color = Color.Magenta)
                            ) {
                                Row(//verticalAlignment = Alignment.CenterVertically,
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        isSearchOpen.let {
                                            mediaViewModel.setSearchVisible(
                                                !it
                                            )
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Search in MediaStore"
                                        )
                                    }

                                    MinimalDropdownMenu(onNav)
                                }
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
            composable<NavSearch> { SearchScreen(onNav={navController.navigate(route = NavScannedResults)}) }
            composable<NavTrackList> { TracksScreen(onNav={navController.navigate(route = NavSearch)}) }
            composable<NavScannedResults> { ViewFileSystemScanResults(onNavOK = {navController.navigate(route = NavMediaScannerResults){
                popUpTo(NavSearch)
            }} , onNavCancel = {navController.navigate(route = NavSearch) })}
            composable<NavMediaScannerResults> { ViewMediaScannerResults(onNav = {}) }
        }
    }


    @Composable
    fun ShowScanResults(results: List<String>){
        LazyColumn {
            for(result in results){
                item(key = result){
                    Text(modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                        text = result)
                }

            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ViewFileSystemScanResults(onNavOK: () -> Unit, onNavCancel: () -> Unit){
        ShowProgressOrContent(mediaViewModel.progressTitle.collectAsState().value) {
            val fileSystemScanResults = mediaViewModel.scanResults.collectAsState().value
//            if(fileSystemScanResults.size == 0){
//                onNavCancel()
//                return@ShowProgressOrContent
//            }
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text( text = "Found files: ${fileSystemScanResults.size}",
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        actions = {
                            Box(modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center) {
                                Button(onClick = { mediaViewModel.runMediaScanner()
                                        onNavOK() }) {
                                    Text("Send to MediaScanner")
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ShowScanResults(fileSystemScanResults)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable fun ShowMediaScannerResults(mediaScannerResults: MediaScannerResults){
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text( text = "Added: ${mediaScannerResults.added} Errors: ${mediaScannerResults.errors}",
                            color = MaterialTheme.colorScheme.onPrimary )
                    }
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                LazyColumn {
                    mediaScannerResults.entriesDescr.forEachIndexed{index, entry ->

                        item {
                            Row {
                                Column(Modifier.padding(horizontal =  2.dp, vertical = 4.dp)) {
                                    Text("$index") }
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    entry.filePath?.let { Text(it) }
                                    Text(entry.contentUri.toString())
                                }
                            }
                        }

                    }
                }
            }
        }

    }

    @Composable
    fun ViewMediaScannerResults(onNav: () -> Unit){
        ShowProgressOrContent(mediaViewModel.progressTitle.collectAsState().value) {
            ShowMediaScannerResults(mediaViewModel.mediaScanResults.collectAsState().value)
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
                        MinimalDropdownMenu(onNav)
                    }
                }
            )
        },
        ) { innerPadding ->

//            ShowPlayList(mediaViewModel.cursor)
        }
    }


}