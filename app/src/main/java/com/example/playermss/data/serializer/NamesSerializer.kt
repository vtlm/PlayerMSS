package com.example.playermss.data.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.playermss.StringSet
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream


object NamesSerializer : Serializer<StringSet.NameList> {
    override val defaultValue: StringSet.NameList = StringSet.NameList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): StringSet.NameList {
        try {
            return StringSet.NameList.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: StringSet.NameList,
        output: OutputStream
    ) = t.writeTo(output)
}