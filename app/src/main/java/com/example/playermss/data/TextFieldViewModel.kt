package com.example.playermss.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TextFieldViewModel(val description: String = ""): ViewModel(){
    private val _textField = MutableStateFlow("")
    val textField: StateFlow<String> = _textField

    fun setText(text: String){
        _textField.value = text
    }
}