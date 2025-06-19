package com.lakshay.arxplorer.ui.paper

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaperViewModel @Inject constructor(
    application: Application,
    private val bookmarkRepository: BookmarkRepository
) : AndroidViewModel(application) {
    private val _currentPaper = MutableStateFlow<ArxivPaper?>(null)
    val currentPaper: StateFlow<ArxivPaper?> = _currentPaper
    
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    fun setPaper(paper: ArxivPaper?) {
        _currentPaper.value = paper
        paper?.let { checkIfBookmarked(it.id) }
    }
    
    private fun checkIfBookmarked(paperId: String) {
        viewModelScope.launch {
            _isBookmarked.value = bookmarkRepository.isBookmarked(paperId)
        }
    }
    
    fun toggleBookmark() {
        val paper = _currentPaper.value ?: return
        viewModelScope.launch {
            if (_isBookmarked.value) {
                bookmarkRepository.removeBookmark(paper.id)
                _isBookmarked.value = false
            } else {
                bookmarkRepository.addBookmark(paper.id)
                _isBookmarked.value = true
            }
        }
    }

    fun downloadPdf(pdfUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(pdfUrl)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun sharePaper(paper: ArxivPaper) {
        val shareText = buildString {
            appendLine(paper.title)
            appendLine()
            appendLine("Authors: ${paper.authors.joinToString(", ")}")
            appendLine()
            appendLine("Abstract:")
            appendLine(paper.abstract)
            appendLine()
            appendLine("arXiv: ${paper.pdfUrl}")
            paper.doi?.let { appendLine("DOI: $it") }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, paper.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val shareIntent = Intent.createChooser(intent, "Share Paper")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(shareIntent)
    }
} 