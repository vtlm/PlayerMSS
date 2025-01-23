package com.example.playermss

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.audiofx.BassBoost
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
//import androidx.compose.material.icons.
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.playermss.ui.theme.PlayerMSSTheme
import com.google.common.util.concurrent.MoreExecutors


//todo
//rotation (sace state, remember)

const val REQ_CODE=0xff0033

@UnstableApi
class MainActivity : ComponentActivity() {

    private var trackList: TrackList? = null
    private var mediaControllerLoaded = mutableStateOf(false)
    private var mediaController:MediaController? = null
    private var cMediaMetadata = mutableStateOf<MediaMetadata?>(null)

//    private var bassBoost: BassBoost? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken =
            SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java))
        val controllerFuture =
            MediaController.Builder(applicationContext, sessionToken).buildAsync()
        controllerFuture.addListener({
            // MediaController is available here with controllerFuture.get()
            mediaController=controllerFuture.get()

            if(trackList == null) {
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

                        cMediaMetadata.value=mediaMetadata

                     }

                }
            )


            mediaControllerLoaded.value=true

        }, MoreExecutors.directExecutor())

//        enableEdgeToEdge()
        setContent {
            PlayerMSSTheme {
               MainUI()
            }
        }
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
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQ_CODE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { directoryUri ->
                // Perform operations on the document using its URI.
                Log.d("DBG",directoryUri.toString())
                val documentsTree = DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return
                trackList!!.addFromFiles(documentsTree.listFiles())
            }
        }
    }

    @OptIn(UnstableApi::class)
    @Composable
fun TrackInfo(mediaMetadata: MediaMetadata?){

        if(mediaMetadata == null){
            Text("Select a track to play")
        }else {
            var songInfo = "";
            mediaMetadata.artist?.let {
                Log.d("DBG", it.toString())
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
            Text(songInfo)
        }
}

@Composable
fun MainUI(){
//    var count by remember { mutableStateOf(0) }
    var repeatMode = remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    val maxRepeatModeInd=2
    val repeatModeId=arrayOf(R.drawable.baseline_repeat_24,R.drawable.baseline_repeat_on_24,R.drawable.baseline_repeat_one_on_24)

    Scaffold(modifier = Modifier.fillMaxSize()//.padding(top = 36.dp)
    ) { innerPadding ->
        Column {
            Row (modifier = Modifier.weight(1f)){
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
            Greeting(
                name = "Android",
                modifier = Modifier
                    .padding(innerPadding)
                    .weight(1f)
            )

            TrackInfo(mediaMetadata = cMediaMetadata.value)

            Row(modifier = Modifier.weight(1f)) {
//                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                IconButton(onClick = { mediaController?.seekToPrevious() }) {
                    Icon(painter = painterResource(id = R.drawable.baseline_skip_previous_24), contentDescription = "Skip to Prev")
                }
                IconButton(onClick = { mediaController?.prepare()
                                        mediaController?.play()
                }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                }
                IconButton(onClick = { mediaController?.pause() }) {
                    Icon(painter = painterResource(id = R.drawable.baseline_pause_24), contentDescription = "Pause")
                }
                IconButton(onClick = { mediaController?.seekToNext() }) {
                    Icon(painter = painterResource(id = R.drawable.baseline_skip_next_24), contentDescription = "Skip to Next")
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
    }
}

}

@Composable
fun trackList(){

}

@Composable
fun ButtonAction(caption:String, onClick: () -> Unit) {
    Button(onClick = { onClick() }) {
        Text(caption)
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlayerMSSTheme {
        Greeting("Android")
    }
}