package com.example.playermss

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.MediaPlayer.TrackInfo
import android.media.audiofx.BassBoost
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.Layout.Alignment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
//import androidx.compose.material.icons.
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
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
    private var trackPosition by mutableLongStateOf(0)
    private var trackDuration by mutableLongStateOf(0)

    //    private var bassBoost: BassBoost? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

//            bassBoost = BassBoost(0,0)//sessionToken.uid)

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

    @kotlin.OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainUI() {
        Scaffold(
            modifier = Modifier.fillMaxSize()//.padding(top = 36.dp)
        ) { innerPadding ->
            Column {
                Row(modifier = Modifier.weight(1f)) {
                    ButtonAction(caption = "Add") {
                        // Code here executes on main thread after user presses button
                        Log.d("DBG", "button clicked")
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
                        }
                        startActivityForResult(intent, REQ_CODE)
                    }
                    ButtonAction(caption = "Clear") {
                        trackList?.clear()
                        mediaController?.clearMediaItems()
                    }
                }

                Row(modifier = Modifier.weight(7f)) {
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
                Box{
                    Row {
                        TrackTime(mediaController)
                    }
                        Row {
                            TrackInfo(
                                mediaMetadata = cMediaMetadata.value,
                                Modifier
                                    .padding(start = 14.dp, top=4.dp)
                                    .width(intrinsicSize = IntrinsicSize.Max)
                                    .basicMarquee()
                                    .weight(4f)
                            )
                            Text("", Modifier.weight(1.2f))
                        }
                }

                PlayControls(mediaController = mediaController, modifier = Modifier.weight(4f))
            }
        }
    }

}

@Composable
fun ButtonAction(caption: String, onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(caption)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TrackInfo(mediaMetadata: MediaMetadata?,modifier: Modifier=Modifier) {

    if (mediaMetadata == null) {
        Text("Select a track to play",modifier)
    } else {
        var songInfo = "";
        mediaMetadata.artist?.let {
            Log.d("DBGC", it.toString())
            songInfo += it.toString()
        }
        mediaMetadata.albumTitle?.let {
            Log.d("DBG", it.toString())
            songInfo += "-"
            songInfo += it.toString()
        }
        mediaMetadata.title?.let {
            Log.d("DBG", it.toString())
            songInfo += "-"
            songInfo += it.toString()
        }
        Text(songInfo,modifier)
    }
}

@Composable
fun TrackTime(mediaController: MediaController?) {

    var currentTime by remember { mutableLongStateOf(0L) }
    var fullTime by remember { mutableLongStateOf(0L) }
    val cMediaMetadata = remember { mutableStateOf<MediaMetadata?>(null) }
    var userInteraction by remember {  mutableIntStateOf(0) }

    LaunchedEffect(mediaController) {

        mediaController?.addListener(
            object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    Log.d("DMG_CR", "CR mediaData changed")
                    cMediaMetadata.value = mediaMetadata
                }
            }
        )

        while (true) {
            Log.d("CRT", "Hello World from LE coroutine $currentTime, userInteraction $userInteraction")
            if ((userInteraction == 0) && mediaController?.isPlaying == true) {
                fullTime = mediaController?.contentDuration!!
                currentTime = mediaController?.currentPosition!!;
            }
            delay(1000)
        }
    }

    var remaining by remember { mutableIntStateOf(0) }
    val calcTime = if (remaining == 1) fullTime - currentTime else currentTime
    val time = "%1\$tM:%1\$tS".format(calcTime)
    val showTime = if (remaining == 1) "-$time" else time
    Log.d("TIME", "$currentTime")

    Column {
        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxSize()
//        .border(width = Dp.Hairline, color = Color.Gray, shape = RectangleShape)//border(width = Dp.Hairline , brush = Brush.,shape=null )
                .padding(horizontal = 2.dp, vertical = 0.dp)
                .clickable(onClick = { remaining = remaining xor 1 }),
            shape = RoundedCornerShape(10),

            ) {
                Row(modifier = Modifier.align(alignment = androidx.compose.ui.Alignment.End)) {
//                    TrackInfo(cMediaMetadata.value)
                    Text(
                        showTime,
                        Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.End,
                        fontSize = 24.sp
                    )
                }
        }
        Slider(
            value = currentTime / 1000F,
            onValueChange = { currentTime = (it * 1000).toLong()
                            userInteraction = 1},
            onValueChangeFinished = { mediaController?.seekTo(currentTime)
                                    userInteraction = 0 },
            valueRange = 0f..fullTime / 1000F
        )
    }
}

@Composable
fun PlayControls(mediaController: MediaController?, modifier: Modifier) {

    Log.d("DBG_PC", "play ctls called")

    val repeatMode = remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    val maxRepeatModeInd = 2
    val repeatModeId = arrayOf(
        R.drawable.baseline_repeat_24,
        R.drawable.baseline_repeat_one_on_24,
        R.drawable.baseline_repeat_on_24,
    )
    Row {//(modifier = Modifier.weight(1f))
        IconButton(onClick = { mediaController?.seekToPrevious() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                contentDescription = "Skip to Prev"
            )
        }
        IconButton(onClick = {
            mediaController?.prepare()
            mediaController?.play()
        }) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
        }
        IconButton(onClick = { mediaController?.pause() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_pause_24),
                contentDescription = "Pause"
            )
        }
        IconButton(onClick = { mediaController?.seekToNext() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_skip_next_24),
                contentDescription = "Skip to Next"
            )
        }
        IconButton(onClick = {
            repeatMode.intValue += 1
            if (repeatMode.intValue > maxRepeatModeInd) {
                repeatMode.intValue = 0
            }
            mediaController?.repeatMode = repeatMode.intValue
        }) {
            Icon(
                painter = painterResource(id = repeatModeId[repeatMode.intValue]),
                contentDescription = "Repeat Mode"
            )
        }
    }

}
