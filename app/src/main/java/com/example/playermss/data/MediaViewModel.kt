package com.example.playermss.data

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//data class ParamDescription(
//    val name: String = "name"
//)

class MediaViewModel: ViewModel() {
    private val _cursor = MutableStateFlow<Cursor?>(null)
    val cursor: StateFlow<Cursor?> = _cursor.asStateFlow()

//    private var __paramStrings = mutableListOf<String>("","","","",)
//    private val _paramStrings = MutableStateFlow(__paramStrings)
//    val paramStrings: MutableStateFlow<MutableList<String>> = _paramStrings//.asStateFlow()
//
//    val paramDescriptions = listOf(ParamDescription("Title"),
//        ParamDescription("Artist"),
//        ParamDescription("Album"),
//        ParamDescription("Year"),
//        )

    lateinit var context: Context


//    fun setParamString(ind: Int, value: String){
//        __paramStrings[ind] = value
//        __paramStrings = __paramStrings.subList(0,3)
//    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun query(queryParams: QueryParams? = QueryParams()){

        viewModelScope.launch {
            val f1 = MediaStore.getExternalVolumeNames(context)
            val contentUri = MediaStore.Audio.Media.getContentUri(f1.elementAt(0))

            _cursor.value = context.contentResolver.query(
                contentUri,
                queryParams?.projection,
                queryParams?.selection,
                queryParams?.selectionArgs,
                queryParams?.sortOrder
            )

            Log.d("MVM", "Items: ${_cursor.value?.count}")
        }
    }

    fun query(searchFields : List<TextFieldViewModel>){
        val queryParams:QueryParams=QueryParams()

        queryParams.selectionArgs[0]=searchFields[1].textField.value
    }

}