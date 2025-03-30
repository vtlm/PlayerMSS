package com.example.playermss.data

data class QueryParams(
    var projection: Array<String> = arrayOf(),
    var selection: String = "",
    var selectionArgs: MutableList<String> = mutableListOf(),
    var sortOrder: String = "")
