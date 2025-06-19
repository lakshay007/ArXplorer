package com.lakshay.arxplorer.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.network.CommentApi
import com.lakshay.arxplorer.data.network.CommentCountRequest
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.BookmarkRepository
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
    private val bookmarkRepository: BookmarkRepository,
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

    private val _bookmarkedPaperIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedPaperIds: StateFlow<Set<String>> = _bookmarkedPaperIds

    private val _currentPaper = MutableStateFlow<ArxivPaper?>(null)
    val currentPaper: StateFlow<ArxivPaper?> = _currentPaper.asStateFlow()

    init {
        // Load bookmarked paper IDs
        viewModelScope.launch {
            bookmarkRepository.loadBookmarkedPaperIds()
            bookmarkRepository.getBookmarkedPaperIds().collect { paperIds ->
                _bookmarkedPaperIds.value = paperIds
            }
        }
    }

    fun initializeData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = UiState.Error("User not authenticated")
                return@launch
            }

            // Check if user has preferences
            if (!arxivRepository.hasUserPreferences(userId)) {
                _showPreferencesScreen.value = true
                return@launch
            }

            // Keep loading state while fetching papers
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
                // Use the full paper ID including the version suffix instead of splitting it
                val paperIds = papers.map { it.id.substringAfterLast('/') }
                val response = commentApi.getCommentCounts(CommentCountRequest(paperIds))
                
                if (response.isSuccessful) {
                    response.body()?.let { apiCounts ->
                        // Create a mapping from full paper IDs to counts
                        val countsWithFullIds = papers.associate { paper ->
                            val shortId = paper.id.substringAfterLast('/') 
                            paper.id to (apiCounts[shortId] ?: 0)
                        }
                        
                        // Update the comment counts with the full paper IDs as keys
                        _commentCounts.value = _commentCounts.value.toMutableMap().apply {
                            putAll(countsWithFullIds)
                        }
                        
                        // Log for debugging
                        Log.d(TAG, "Paper IDs sent to API: $paperIds")
                        Log.d(TAG, "API response counts: $apiCounts")
                        Log.d(TAG, "Mapped to full IDs: $countsWithFullIds")
                        Log.d(TAG, "Updated comment counts: ${_commentCounts.value}")
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
            _uiState.value = UiState.Loading  // Add loading state here too
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
                val isTitleSearch = sortBy == "title"
                val result = arxivRepository.searchArxiv(
                    query = query,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    isTitleSearch = isTitleSearch
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

    fun toggleBookmark(paperId: String) {
        viewModelScope.launch {
            if (_bookmarkedPaperIds.value.contains(paperId)) {
                bookmarkRepository.removeBookmark(paperId)
            } else {
                bookmarkRepository.addBookmark(paperId)
            }
        }
    }

    fun isBookmarked(paperId: String): Boolean {
        return _bookmarkedPaperIds.value.contains(paperId)
    }

    fun setCurrentPaper(paper: ArxivPaper) {
        _currentPaper.value = paper
    }
} 