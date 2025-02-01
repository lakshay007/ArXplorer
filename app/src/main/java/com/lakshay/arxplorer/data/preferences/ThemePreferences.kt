package com.lakshay.arxplorer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")

    val isDarkTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[isDarkThemeKey] ?: false
        }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[isDarkThemeKey] = isDark
        }
    }
} 