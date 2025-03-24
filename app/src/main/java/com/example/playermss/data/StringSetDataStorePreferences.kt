package com.example.playermss.data

import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StringSetDataStorePreferences (
    keyName: String,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val launchScope: CoroutineScope
    ){

    private val key = stringSetPreferencesKey(keyName)

    private fun toggleInSet(inSet: Set<String>, name: String) : Set<String>{
        return if(inSet.contains(name)){
            inSet.minus(name)
        }else{
            inSet.plus(name)
        }
    }

    fun toggle(name: String){
        val newSet = toggleInSet(localStringSet.value, name)
        launchScope.launch {
            userPreferencesRepository.setStringSet(key, newSet)
        }
    }

    fun add(name: String?){
        if(name != null) {
            if (!localStringSet.value.contains(name)) {
                val newSet = localStringSet.value.plus(name)
                launchScope.launch {
                    userPreferencesRepository.setStringSet(key, newSet)
                }
            }
        }
    }

    private val stringSet: SharedFlow<Set<String>> =
        userPreferencesRepository.getStringSet(key)
            .shareIn(
                scope = launchScope,
                started = SharingStarted.Eagerly// WhileSubscribed(5_000),
            )

//    val __stringSet: StateFlow<Set<String>> =
//        userPreferencesRepository.getStringSet(key)
//            .stateIn(
//                scope = launchScope,
//                started = SharingStarted.WhileSubscribed(5_000),
//                initialValue = runBlocking {
//                    val c = userPreferencesRepository.getStringSet(key).first()
//                    c
//                }
//            )

    private var localStringSet = stringSet.stateIn(
        scope = launchScope,
        started = SharingStarted.Eagerly,//WhileSubscribed(5_000),
        initialValue = runBlocking {
            val c = userPreferencesRepository.getStringSet(key).first()
            c
        }
    )

    val externalStringSet = stringSet.stateIn(
        scope = launchScope,
        started = SharingStarted.Eagerly,// WhileSubscribed(5_000),
        initialValue = runBlocking {
            val c = userPreferencesRepository.getStringSet(key).first()
            c
        }
    )


    }