package com.example.playermss.data.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.playermss.Messages.MediaTrackDataList
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream


object MediaTrackDataListSerializer : Serializer<MediaTrackDataList> {
    override val defaultValue: MediaTrackDataList = MediaTrackDataList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MediaTrackDataList {
        try {
            return MediaTrackDataList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: MediaTrackDataList,
        output: OutputStream
    ) = t.writeTo(output)
}


