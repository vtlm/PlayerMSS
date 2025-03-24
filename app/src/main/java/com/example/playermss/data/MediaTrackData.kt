package com.example.playermss.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem

val audioColumns = arrayOf(
    MediaStore.Audio.AudioColumns._ID,
    MediaStore.Audio.AudioColumns.DATA,
    MediaStore.Audio.AudioColumns.ARTIST,
    MediaStore.Audio.AudioColumns.ALBUM,
    MediaStore.Audio.AudioColumns.TITLE,
    MediaStore.Audio.AudioColumns.YEAR,
    MediaStore.Audio.AudioColumns.TRACK,
    MediaStore.Audio.AudioColumns.DURATION,
)

class MediaTrackData(
    val artist: String = (""),
    val album: String = (""),
    val title: String = (""),
    val year: Int? = 0,
    val track: Int? = 0,
    val duration: Int? = 0,
    val uri: Uri? = null,
){
    fun getSystemId():Int{
        return uri.hashCode()
    }
}

private fun yearFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Int? {

    val patterns= arrayOf(
        "(19|20)\\d{2}",
        "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
        "(3[01]|[12][0-9]|0[1-9]|[1-9])/(1[0-2]|0[1-9]|[1-9])/[0-9]{4}",
        "(3[01]|[12][0-9]|0[1-9]|[1-9])\\.(1[0-2]|0[1-9]|[1-9])\\.[0-9]{2}",
    )

    var year = columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getInt(it) }

    if (year == null || year == 0) {
        val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
            .toString()

        for (pattern in patterns) {
            val yearPattern = Regex(pattern)
            val res = yearPattern.find(data)
            if (res != null) {
                year = res.value.toIntOrNull()
                return year
            }
        }
    }
    return year
}

private fun trackNumberFromCursor(cursor: Cursor, columnMap: Map<String,Int?>): Int? {

    val patterns= arrayOf(
        "d{2}",
        //"\\s/d{2}\\s",
        "[0-9]{2}",
        // "\\s/[0-9]{2}\\s",
    )

    var trackNumber = columnMap[MediaStore.Audio.AudioColumns.TRACK]?.let { cursor.getInt(it) }

    if (trackNumber == null || trackNumber == 0) {
        val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }
            .toString().split('/').last()

        for (pattern in patterns) {
            val res = Regex(pattern).findAll(data)
            if (res.count() > 0) {
                trackNumber = res.last().value.toIntOrNull()
                return trackNumber
            }else{
                trackNumber = null
            }
        }
    }

    return trackNumber
}

@RequiresApi(Build.VERSION_CODES.Q)
fun uriFromCursor(cursor: Cursor, columnMap: Map<String,Int?>, context: Context): Uri?{
    val id = columnMap[MediaStore.Audio.AudioColumns._ID]?.let { cursor.getLong(it) }
    val data = columnMap[MediaStore.Audio.AudioColumns.DATA]?.let { cursor.getString(it) }

    val externalVolumeNames = MediaStore.getExternalVolumeNames(context)//.map { it.uppercase(context.resources.configuration.locales[0]) }

    if(data != null){
        val externalVolumeName = externalVolumeNames.filter { data.uppercase(context.resources.configuration.locales[0])
            .contains(
                it.uppercase(context.resources.configuration.locales[0])
            ) }

        if(externalVolumeName.count() == 1){

            val volumeName = externalVolumeName[0]

            if(volumeName != null){

                val contentUri = MediaStore.Audio.Media.getContentUri(volumeName)
                val itemUri = id?.let { ContentUris.withAppendedId(contentUri, it) }

                val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(data))
                val mediaItem2: MediaItem?= itemUri?.let { MediaItem.fromUri(it) }
//    if(id != null && data != null){
//        return
//    }
                return itemUri

            }
        }
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.Q)
fun cursorToMediaTrackData(cursor: Cursor, columnMap: Map<String,Int>, context: Context): MediaTrackData{

//        val dt = columnMap[MediaStore.Audio.AudioColumns.DURATION]?.let { cursor.getType(it) }
//        val dt1 = columnMap[MediaStore.Audio.AudioColumns.YEAR]?.let { cursor.getType(it) }
//        val dt2 = columnMap[MediaStore.Audio.AudioColumns.TRACK]?.let { cursor.getType(it) }

    val rv = MediaTrackData(
        columnMap[MediaStore.Audio.AudioColumns.ARTIST]?.let { cursor.getString(it) }.toString(),
        columnMap[MediaStore.Audio.AudioColumns.ALBUM]?.let { cursor.getString(it) }.toString(),
        columnMap[MediaStore.Audio.AudioColumns.TITLE]?.let { cursor.getString(it) }.toString(),
        yearFromCursor(cursor, columnMap),
        trackNumberFromCursor(cursor, columnMap),
        columnMap[MediaStore.Audio.AudioColumns.DURATION]?.let { cursor.getInt(it) },
        uri = uriFromCursor(cursor, columnMap, context)
    )
    return rv
}
