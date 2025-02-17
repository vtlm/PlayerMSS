package com.example.playermss

import android.app.Activity
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.MediaPlayer.TrackInfo
import android.media.audiofx.BassBoost
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Layout.Alignment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
//import androidx.compose.material.icons.
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.ui.theme.PlayerMSSTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timerTask

//todo
//rotation (face state, remember)

const val REQ_CODE = 0xff0033

@UnstableApi
class MainActivity : ComponentActivity() {

    private var trackList: TrackList? = null
    private var mediaControllerLoaded = mutableStateOf(false)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var cMediaMetadata = mutableStateOf<MediaMetadata?>(null)

        private var bassBoost: BassBoost? = null
    private  var visualizer: Visualizer? = null
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

// Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher. You can use either a val, as shown in this snippet,
// or a lateinit var in your onAttach() or onCreate() method.
        val requestPermissionLauncher1 =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            }
        requestPermissionLauncher1.launch(android.Manifest.permission.RECORD_AUDIO)

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

        requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))

        val r= ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.READ_MEDIA_AUDIO)
        val r1= ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.RECORD_AUDIO)



        val f=getExternalFilesDirs(null)
        val f1= MediaStore.getExternalVolumeNames(applicationContext)
        val cu= MediaStore.Audio.Media.getContentUri(f1.elementAt(1))

        this.contentResolver.query(
            cu,
            null,
            null,
            null,
            null,
        )?.use {
                cursor ->
            val  c=cursor.count;
            val a=cursor.columnCount;

        }

        MediaStore.getMediaScannerUri()

        val cu1= MediaStore.Audio.Media.getContentUriForPath(f1.elementAt(0))
//        val collection1 = Uri.parse("content://storage/9C33-6BBD/Music");
        val collection1 = MediaStore.Files.getContentUri(
//                    MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
//        this.contentResolver.insert(collection1,null)
        val query1 = this.contentResolver.query(
            cu,//ollection1,
            null,
            null,
            null,
            null,
//            projection,
//            selection,
//            selectionArgs,
//            sortOrder
        )
        query1?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

            }
        }


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
                MainUI()
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

    @OptIn(UnstableApi::class)
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQ_CODE
            && resultCode == Activity.RESULT_OK
        ) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { directoryUri ->
                // Perform operations on the document using its URI.
                Log.d("DBG", directoryUri.toString())
                val documentsTree =
                    DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return
                trackList!!.addFromFiles(documentsTree.listFiles())
            }
        }
    }

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
                        startActivityForResult(intent, REQ_CODE)
                        expanded = false
                    }
                )
            }
        }
    }

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
