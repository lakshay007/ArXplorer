package com.lakshay.arxplorer.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.arxplorer.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    val isDarkTheme: StateFlow<Boolean> = themePreferences.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun toggleTheme() {
        viewModelScope.launch {
            themePreferences.setDarkTheme(!isDarkTheme.value)
        }
    }
} 