package com.lakshay.arxplorer.data.repository

import com.lakshay.arxplorer.data.network.*
import com.lakshay.arxplorer.ui.components.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentApi: CommentApi
) {
    fun getComments(paperId: String, token: String): Flow<List<Comment>> = flow {
        val response = commentApi.getComments(paperId, "Bearer $token")
        if (response.isSuccessful) {
            response.body()?.let { comments ->
                // Convert all comments to UI models, preserving the nested structure
                val rootComments = comments.map { it.toUiModel() }
                emit(rootComments)
            }
        } else {
            throw Exception("Failed to fetch comments: ${response.message()}")
        }
    }

    private fun CommentResponse.toUiModel(): Comment {
        val upvoters = userVotes.filter { it.vote == 1 }.map { it.user }
        val downvoters = userVotes.filter { it.vote == -1 }.map { it.user }
        
        return Comment(
            id = _id,
            userId = author._id,
            userName = author.name,
            userPhotoUrl = author.profilePicture,
            content = content,
            parentId = parentId,
            upvotes = upvoters,
            downvotes = downvoters,
            createdAt = ZonedDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME),
            replies = replies?.map { it.toUiModel() } ?: emptyList()  // Recursively convert nested replies
        )
    }

    suspend fun addComment(
        paperId: String,
        content: String,
        parentId: String?,
        token: String
    ): Comment {
        val clientId = UUID.randomUUID().toString()
        val request = CommentRequest(paperId, content, parentId, clientId)
        val response = commentApi.addComment(request, "Bearer $token")
        
        if (response.isSuccessful) {
            return response.body()?.toUiModel() ?: throw Exception("Empty response body")
        } else {
            throw Exception("Failed to add comment: ${response.message()}")
        }
    }

    suspend fun voteComment(commentId: String, isUpvote: Boolean, token: String): Int {
        val vote = if (isUpvote) 1 else -1
        val response = commentApi.voteComment(commentId, VoteRequest(vote), "Bearer $token")
        
        if (response.isSuccessful) {
            return response.body()?.votes ?: throw Exception("Empty response body")
        } else {
            throw Exception("Failed to vote on comment: ${response.message()}")
        }
    }

    suspend fun deleteComment(commentId: String, token: String) {
        val response = commentApi.deleteComment(commentId, "Bearer $token")
        if (!response.isSuccessful) {
            throw Exception("Failed to delete comment: ${response.message()}")
        }
    }
} 