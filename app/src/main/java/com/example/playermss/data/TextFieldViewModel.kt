package com.example.playermss.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TextFieldViewModel @Inject constructor
    (
    private val userPreferencesRepository: UserPreferencesRepository,
//    val description: String = ""
): ViewModel(){
    private val _textField = MutableStateFlow("")
    val textField: StateFlow<String> = _textField

    fun setText(text: String){
        _textField.value = text
    }
}