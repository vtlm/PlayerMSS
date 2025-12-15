package com.example.playermss.data

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    private val userInputDictKey = stringSetPreferencesKey(description+"UserDict")
    private val savedText: StateFlow<String> = userPreferencesRepository.getOrDefaultAsStateFlow(key,launchScope,"")
    private val savedUserInputDict: StateFlow<Set<String>> = userPreferencesRepository.getOrDefaultAsStateFlow(userInputDictKey,launchScope,setOf())
    private val _text = MutableStateFlow(savedText.value)
    val text: StateFlow<String> = _text.asStateFlow()
    private val _userInputDict = MutableStateFlow(savedUserInputDict.value)
    val userInputDict: StateFlow<Set<String>> = _userInputDict.asStateFlow()

    fun setText(text: String){
        _text.value = text
    }

    fun userInputDictIsNotEmpty(): Boolean {
        return userInputDict.value.any { it.isNotBlank() }
    }

    fun userInputDictFilteredByCurrentText():List<String>{
        return userInputDict.value
            .filter { it.contains(text.value) }
            .filter { it.isNotBlank() }
            .sortedBy { it }
    }

    fun saveText() {
        userPreferencesRepository.set(key, launchScope, _text.value)
        updateUserInputDict()
        saveUserInputDict()
    }

    fun addToUserInputDict(text: String){
        _userInputDict.value += text
    }

    fun updateUserInputDict(){
        addToUserInputDict(_text.value)
    }

    fun saveUserInputDict() = userPreferencesRepository.set(userInputDictKey, launchScope,userInputDict.value)
}