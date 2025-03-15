package com.example.playermss.data

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class QueryTextField (
    val description: String,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val launchScope: CoroutineScope
){
    private val key = stringPreferencesKey(description)

    val savedText: StateFlow<String> =
        userPreferencesRepository.getText(key)
            .stateIn(
                scope = launchScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = runBlocking {
                    userPreferencesRepository.getText(key).first()
                }
            )

    private val _text = MutableStateFlow(savedText.value)
    val text: StateFlow<String> = _text.asStateFlow()

    fun setText(text: String){
        _text.value = text
    }

    fun saveText() {
        launchScope.launch {
            userPreferencesRepository.setText(key, _text.value)
        }
    }

}