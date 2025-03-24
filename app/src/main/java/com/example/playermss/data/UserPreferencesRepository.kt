
package com.example.playermss.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val IS_LINEAR_LAYOUT = booleanPreferencesKey("is_linear_layout")
        val IS_REMAIN_TIME = booleanPreferencesKey("is_remain_time")
        val PLAYER_REPEAT_MODE = intPreferencesKey("player_repeat_mode")
        const val TAG = "UserPreferencesRepo"
    }

    val isLinearLayout: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[IS_LINEAR_LAYOUT] ?: true
        }

    suspend fun saveLayoutPreference(isLinearLayout: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LINEAR_LAYOUT] = isLinearLayout
        }
    }

    val isRemainTime: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[IS_REMAIN_TIME] ?: false
        }

    suspend fun saveRemainTimePreference(isRemainTime: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_REMAIN_TIME] = isRemainTime
        }
    }

    val playerRepeatMode: Flow<Int> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[PLAYER_REPEAT_MODE] ?: 0
        }

    suspend fun savePlayerRepeatModePreference(playerRepeatMode: Int) {
        dataStore.edit { preferences ->
            preferences[PLAYER_REPEAT_MODE] = playerRepeatMode
        }
    }

    fun getInt(key: Preferences.Key<Int>): Flow<Int>{
        val rvNumber = dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(TAG, "Error reading preferences.", it)
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }
            .map { preferences ->
                preferences[key] ?: 0
            }
        return rvNumber
    }

    suspend fun setInt(key: Preferences.Key<Int>, number: Int?){
        if(number != null) {
            dataStore.edit { preferences ->
                preferences[key] = number
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
