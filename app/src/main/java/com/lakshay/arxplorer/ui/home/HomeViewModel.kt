package com.lakshay.arxplorer.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.network.CommentApi
import com.lakshay.arxplorer.data.network.CommentCountRequest
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.TimePeriod
import com.lakshay.arxplorer.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val arxivRepository: ArxivRepository,
    private val commentApi: CommentApi
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<UiState<List<ArxivPaper>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArxivPaper>>> = _uiState.asStateFlow()

    private val _showPreferencesScreen = MutableStateFlow(false)
    val showPreferencesScreen: StateFlow<Boolean> = _showPreferencesScreen.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _commentCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentCounts: StateFlow<Map<String, Int>> = _commentCounts.asStateFlow()

    private var currentMode = "new"  // Track current mode

    fun initializeData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            // Check if user has preferences
            if (!arxivRepository.hasUserPreferences(userId)) {
                _showPreferencesScreen.value = true
                _uiState.value = UiState.Empty
                return@launch
            }

            // Load new papers
            arxivRepository.fetchPapersForUserPreferences(userId).fold(
                onSuccess = { papers ->
                    _uiState.value = UiState.Success(papers)
                    loadCommentCounts(papers)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadPapers() {
        currentMode = "new"  // Set mode to new
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            // Clear any remaining papers from previous mode
            arxivRepository.clearRemainingPapers()
            
            arxivRepository.fetchPapersForUserPreferences(userId).fold(
                onSuccess = { papers ->
                    if (papers.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(papers)
                        loadCommentCounts(papers)
                    }
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load papers")
                }
            )
        }
    }

    fun loadMorePapers() {
        viewModelScope.launch {
            if (_isLoadingMore.value) return@launch
            _isLoadingMore.value = true

            try {
                val currentPapers = (_uiState.value as? UiState.Success)?.data ?: emptyList()
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _isLoadingMore.value = false
                    return@launch
                }

                when (currentMode) {
                    "new" -> {
                        // For new papers, use getNextBatchOfPapers which uses the cached papers
                        val morePapers = arxivRepository.getNextBatchOfPapers()
                        if (morePapers.isNotEmpty()) {
                            _uiState.value = UiState.Success(currentPapers + morePapers)
                            loadCommentCounts(morePapers)
                        }
                    }
                    "top" -> {
                        // For top papers, check if there are any remaining papers
                        if (arxivRepository.hasMoreTopPapers()) {
                            arxivRepository.getNextBatchOfTopPapers().fold(
                                onSuccess = { morePapers ->
                                    if (morePapers.isNotEmpty()) {
                                        _uiState.value = UiState.Success(currentPapers + morePapers)
                                        loadCommentCounts(morePapers)
                                    }
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Error loading more top papers", error)
                                }
                            )
                        } else {
                            // No more top papers to load
                            Log.d(TAG, "No more top papers available")
                        }
                    }
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadCommentCounts(papers: List<ArxivPaper>) {
        viewModelScope.launch {
            try {
                val paperIds = papers.map { it.id.substringAfterLast('/').split("v")[0] }
                val response = commentApi.getCommentCounts(CommentCountRequest(paperIds))
                
                if (response.isSuccessful) {
                    response.body()?.let { newCounts ->
                        // Merge the maps properly
                        _commentCounts.value = _commentCounts.value.toMutableMap().apply {
                            putAll(newCounts)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently - counts will show as 0
                Log.e(TAG, "Error loading comment counts", e)
            }
        }
    }

    fun refreshPapers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _commentCounts.value = emptyMap()
                _showPreferencesScreen.value = false
                
                // Clear any remaining papers before refreshing
                arxivRepository.clearRemainingPapers()
                
                if (currentMode == "top") {
                    // Re-fetch top papers with current time period
                    loadTopPapers(TimePeriod.THIS_WEEK) // Default to THIS_WEEK when refreshing top papers
                } else {
                    // For new papers, use initializeData
                    currentMode = "new"
                    initializeData()
                }
                
                // Wait a minimum time to show the refresh animation
                delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun searchPapers(
        query: String,
        sortBy: String = "relevance",
        sortOrder: String = "descending"
    ) {
        if (query.isBlank()) {
            refreshPapers() // If query is empty, show normal feed
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = arxivRepository.searchArxiv(
                    query = query,
                    sortBy = sortBy,
                    sortOrder = sortOrder
                )
                result.fold(
                    onSuccess = { papers ->
                        if (papers.isEmpty()) {
                            _uiState.value = UiState.Empty
                        } else {
                            _uiState.value = UiState.Success(papers)
                            loadCommentCounts(papers)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "Unknown error occurred")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun loadTopPapers(timePeriod: TimePeriod) {
        currentMode = "top"  // Set mode to top
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            arxivRepository.fetchTopPapersForUserPreferences(userId, timePeriod).fold(
                onSuccess = { papers ->
                    _uiState.value = UiState.Success(papers)
                    loadCommentCounts(papers)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
} 