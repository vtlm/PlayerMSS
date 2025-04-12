package com.example.playermss.data

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QueryTextField (
    val description: String,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val launchScope: CoroutineScope
){
    private val key = stringPreferencesKey(description)
    private val savedText: StateFlow<String> = userPreferencesRepository.getOrDefaultAsStateFlow(key,launchScope,"")
    private val _text = MutableStateFlow(savedText.value)
    val text: StateFlow<String> = _text.asStateFlow()

    fun setText(text: String){
        _text.value = text
    }

    fun saveText() = userPreferencesRepository.set(key, launchScope, _text.value)
}