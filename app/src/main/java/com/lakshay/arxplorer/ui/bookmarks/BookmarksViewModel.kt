package com.lakshay.arxplorer.ui.bookmarks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.network.CommentApi
import com.lakshay.arxplorer.data.network.CommentCountRequest
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.BookmarkRepository
import com.lakshay.arxplorer.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BookmarksViewModel"

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val arxivRepository: ArxivRepository,
    private val commentApi: CommentApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<List<ArxivPaper>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArxivPaper>>> = _uiState
    
    private val _bookmarkedPapers = MutableStateFlow<List<ArxivPaper>>(emptyList())
    val bookmarkedPapers: StateFlow<List<ArxivPaper>> = _bookmarkedPapers
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    
    private val _commentCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentCounts: StateFlow<Map<String, Int>> = _commentCounts
    
    private val _currentPaper = MutableStateFlow<ArxivPaper?>(null)
    val currentPaper: StateFlow<ArxivPaper?> = _currentPaper
    
    init {
        loadBookmarks()
    }
    
    fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // First, load all bookmarked paper IDs
                bookmarkRepository.loadBookmarkedPaperIds()
                
                // Then collect the stream of bookmarked paper IDs
                bookmarkRepository.getBookmarkedPaperIds().collectLatest { paperIds ->
                    if (paperIds.isEmpty()) {
                        _bookmarkedPapers.value = emptyList()
                        _uiState.value = UiState.Success(emptyList())
                        return@collectLatest
                    }
                    
                    // Fetch details for each paper ID
                    val papers = mutableListOf<ArxivPaper>()
                    for (paperId in paperIds) {
                        try {
                            val paper = arxivRepository.getPaperById(paperId)
                            paper?.let { papers.add(it) }
                        } catch (e: Exception) {
                            // Skip papers that fail to load
                        }
                    }
                    
                    _bookmarkedPapers.value = papers
                    _uiState.value = UiState.Success(papers)
                    
                    // Load comment counts for the fetched papers
                    loadCommentCounts(papers)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun loadCommentCounts(papers: List<ArxivPaper>) {
        viewModelScope.launch {
            try {
                // Create a map to track the relationship between short and full IDs
                val shortToFullIdMap = mutableMapOf<String, String>()
                
                // Extract the short ID (without version) from each paper
                val paperShortIds = papers.map { paper ->
                    val shortId = paper.id.substringAfterLast('/')
                    shortToFullIdMap[shortId] = paper.id  // Store the mapping
                    shortId
                }
                
                // Log the IDs before the API call for debugging
                Log.d(TAG, "Paper short IDs sent to API: $paperShortIds")
                Log.d(TAG, "ID mapping: $shortToFullIdMap")
                
                // Make the API call with short IDs
                val response = commentApi.getCommentCounts(CommentCountRequest(paperShortIds))
                
                if (response.isSuccessful) {
                    response.body()?.let { apiCounts ->
                        // Log the API response
                        Log.d(TAG, "API response counts: $apiCounts")
                        
                        // Create map using the full paper IDs by looking up in our mapping
                        val updatedCounts = mutableMapOf<String, Int>()
                        apiCounts.forEach { (shortId, count) ->
                            // Get the corresponding full ID or fall back to the short ID
                            val fullId = shortToFullIdMap[shortId] ?: shortId
                            updatedCounts[fullId] = count
                        }
                        
                        // Update the comment counts state
                        _commentCounts.value = updatedCounts
                        
                        // Log the final comment counts
                        Log.d(TAG, "Updated comment counts: ${_commentCounts.value}")
                    }
                } else {
                    Log.e(TAG, "Error from API: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                // Handle error silently - counts will show as 0
                Log.e(TAG, "Error loading comment counts", e)
            }
        }
    }
    
    fun refreshBookmarks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                bookmarkRepository.loadBookmarkedPaperIds()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun removeBookmark(paperId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(paperId)
            
            // Update the UI immediately
            val currentPapers = _bookmarkedPapers.value.toMutableList()
            val updatedPapers = currentPapers.filterNot { it.id == paperId }
            _bookmarkedPapers.value = updatedPapers
            _uiState.value = UiState.Success(updatedPapers)
        }
    }
    
    fun setCurrentPaper(paper: ArxivPaper) {
        _currentPaper.value = paper
    }
} 