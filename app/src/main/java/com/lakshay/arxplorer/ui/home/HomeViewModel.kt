package com.lakshay.arxplorer.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.TimePeriod
import com.lakshay.arxplorer.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "HomeViewModel"

class HomeViewModel : ViewModel() {
    private val repository = ArxivRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<UiState<List<ArxivPaper>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArxivPaper>>> = _uiState.asStateFlow()

    private val _showPreferencesScreen = MutableStateFlow(false)
    val showPreferencesScreen: StateFlow<Boolean> = _showPreferencesScreen.asStateFlow()

    init {
        viewModelScope.launch {
            checkPreferencesAndLoadPapers()
        }
    }

    private suspend fun checkPreferencesAndLoadPapers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = UiState.Error("User not authenticated")
            return
        }

        // Check if user has preferences
        if (!repository.hasUserPreferences(userId)) {
            _showPreferencesScreen.value = true
            return
        }

        loadPapers()
    }

    fun loadPapers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            repository.fetchPapersForUserPreferences(userId).fold(
                onSuccess = { papers ->
                    _uiState.value = UiState.Success(papers)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadTopPapers(timePeriod: TimePeriod) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            repository.fetchTopPapersForUserPreferences(userId, timePeriod).fold(
                onSuccess = { papers ->
                    _uiState.value = UiState.Success(papers)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun refreshPapers() {
        viewModelScope.launch {
            _showPreferencesScreen.value = false
            checkPreferencesAndLoadPapers()
        }
    }
} 