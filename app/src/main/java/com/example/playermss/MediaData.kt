package com.example.playermss

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

@UnstableApi
class MediaData(val uri: Uri, private val context: Context, private val onDataReady: () -> Unit) {

    private val mediaItem = MediaItem.Builder().setUri(uri).build();
    private val trackGroupsFuture = MetadataRetriever.retrieveMetadata(context, mediaItem)
    private var trackGroup: TrackGroup? = null
    var requestDone: Int = 0
    lateinit var titleString: String
    var listIndex: Int = 0
    private var durationMs: Int? = 0
    var pic: ByteArray? = null

    val mediaData = mutableMapOf<String, String>()

    fun requestMediaData() {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, uri)
        durationMs =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toInt()

        pic=mediaMetadataRetriever.embeddedPicture;

        Futures.addCallback(
            trackGroupsFuture,
            object : FutureCallback<TrackGroupArray?> {
                override fun onSuccess(trackGroups: TrackGroupArray?) {
                    Log.d("DBG_H", "mediaItem: $mediaItem.toString()")
                    if (trackGroups != null && (trackGroups.length > 0)) {//handleMetadata(trackGroups)
                        //trackGroups.length
                        trackGroup = trackGroups[0]
                        val format = trackGroup!!.getFormat(0)
                        val metadata = format.metadata
//                        var resStr: String = ""
                        for (i in 0..<metadata!!.length()) {
//                            Log.d("DBG_M", metadata.get(i).toString())
                            val ti: TextInformationFrame? = metadata.get(i) as? TextInformationFrame
//                            Log.d("DBG_MD", "id: ${ti?.id} ${ti?.values?.get(0)}")
                            mediaData[ti?.id.toString()] = ti?.values?.get(0).toString()
                        }
                    }
                    requestDone = 1
                    onDataReady()
                }

                override fun onFailure(t: Throwable) {
                    //handleFailure(t)
                    Log.d("DBG_HF", "Failed to retrieve mediadata")
                }
            },
            context.mainExecutor
        )
    }

    fun asString(): String = titleString

    fun mediaData(k: String, separator: String = ""): String {
        if (mediaData[k] != null) {
            return mediaData[k].toString() + separator
        } else {
            return separator
        }
//        return (mediaData[k] != null ? String?():mediaData[k])
    }

    fun duration():String{
        val durationS = durationMs?.div(1000)
        val mins = durationS?.div(60)
        val secs = durationS?.rem(60)
        return mins.toString()+":"+secs.toString().padStart(2,'0')
//        return "$mins:$secs"
    }
}
