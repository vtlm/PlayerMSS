package com.example.playermss.data

data class QueryParams(
    var projection: Array<String> = arrayOf(),
    var selection: String = "",
    var selectionArgs: Array<String> = arrayOf(),
    var sortOrder: String = "")
