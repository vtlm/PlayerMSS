
package com.example.playermss.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        const val TAG = "UserPreferencesRepo"
    }

    fun <T>getOrDefaultAsStateFlow(key: Preferences.Key<T>, coroutineScope: CoroutineScope, defValue: T): StateFlow<T> {
        val rv = dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[key] ?: defValue
            }
        val rvAsStateFlow = rv
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = runBlocking {
                    rv.first()
                }
            )

        return rvAsStateFlow
    }

    fun <T>getOrDefault(key: Preferences.Key<T>, defaultT: T): Flow<T>{
        val t = dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultT
            }
        return t
    }

    fun <T>set(key: Preferences.Key<T>, coroutineScope: CoroutineScope, t: T?){
        t?.let {
            coroutineScope.launch {
                dataStore.edit { preferences ->
                    preferences[key] = it
                }
            }
        }
    }


    fun getText(key: Preferences.Key<String>): Flow<String>{
        val text = dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[key] ?: ""
            }
        return text
    }

    suspend fun setText(key: Preferences.Key<String>, text: String){
        dataStore.edit { preferences ->
            preferences[key] = text
        }
    }

    fun getStringSet(key: Preferences.Key<Set<String>>): Flow<Set<String>>{
        val stringSet = dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[key] ?: setOf()
            }
        return stringSet
    }

    suspend fun setStringSet(key: Preferences.Key<Set<String>>, stringSet: Set<String>){
        dataStore.edit { preferences ->
            preferences[key] = stringSet
        }
    }
}
