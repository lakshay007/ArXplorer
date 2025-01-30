package com.lakshay.arxplorer.ui.paper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.ui.components.Comment
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.CommentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PaperCommentsViewModel @AssistedInject constructor(
    @Assisted private val paperId: String,
    private val arxivRepository: ArxivRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _paper = MutableStateFlow<ArxivPaper?>(null)
    val paper: StateFlow<ArxivPaper?> = _paper

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadPaper()
        loadComments()
    }

    private fun loadPaper() {
        viewModelScope.launch {
            try {
                val paperData = arxivRepository.getPaperById(paperId)
                _paper.value = paperData
            } catch (e: Exception) {
                _error.value = "Failed to load paper: ${e.message}"
            }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            commentRepository.getComments(paperId, getAuthToken())
                .catch { e ->
                    _error.value = "Failed to load comments: ${e.message}"
                }
                .collect { commentsList ->
                    _comments.value = commentsList
                }
        }
    }

    fun addComment(content: String, userId: String, userName: String, userPhotoUrl: String) {
        viewModelScope.launch {
            try {
                commentRepository.addComment(
                    paperId = paperId,
                    content = content,
                    parentId = null,
                    token = getAuthToken()
                )
                loadComments() // Refresh comments after adding
            } catch (e: Exception) {
                _error.value = "Failed to add comment: ${e.message}"
            }
        }
    }

    fun addReply(
        parentCommentId: String,
        content: String,
        userId: String,
        userName: String,
        userPhotoUrl: String
    ) {
        viewModelScope.launch {
            try {
                commentRepository.addComment(
                    paperId = paperId,
                    content = content,
                    parentId = parentCommentId,
                    token = getAuthToken()
                )
                loadComments() // Refresh comments after adding reply
            } catch (e: Exception) {
                _error.value = "Failed to add reply: ${e.message}"
            }
        }
    }

    fun toggleUpvote(commentId: String, userId: String) {
        viewModelScope.launch {
            try {
                commentRepository.voteComment(
                    commentId = commentId,
                    isUpvote = true,
                    token = getAuthToken()
                )
                loadComments() // Refresh comments after voting
            } catch (e: Exception) {
                _error.value = "Failed to upvote: ${e.message}"
            }
        }
    }

    fun toggleDownvote(commentId: String, userId: String) {
        viewModelScope.launch {
            try {
                commentRepository.voteComment(
                    commentId = commentId,
                    isUpvote = false,
                    token = getAuthToken()
                )
                loadComments() // Refresh comments after voting
            } catch (e: Exception) {
                _error.value = "Failed to downvote: ${e.message}"
            }
        }
    }

    private suspend fun getAuthToken(): String {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.getIdToken(false).await().token ?: ""
            } else {
                throw Exception("User not authenticated")
            }
        } catch (e: Exception) {
            _error.value = "Failed to get auth token: ${e.message}"
            ""
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(paperId: String): PaperCommentsViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: Factory,
            paperId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(paperId) as T
            }
        }
    }
} 