package com.example.playermss

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/*
 * Custom app entry point for manual dependency injection
 */
private const val LAYOUT_PREFERENCE_NAME = "layout_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = LAYOUT_PREFERENCE_NAME
)


class PlayerMSSReleaseApplication: Application() {
    lateinit var userPreferencesRepository: com.example.playermss.data.UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = com.example.playermss.data.UserPreferencesRepository(dataStore)
    }
}