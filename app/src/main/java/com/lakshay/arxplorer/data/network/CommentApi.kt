package com.lakshay.arxplorer.data.network

import com.lakshay.arxplorer.ui.components.Comment
import retrofit2.Response
import retrofit2.http.*
import java.time.ZonedDateTime

interface CommentApi {
    @GET("api/comments/{paperId}")
    suspend fun getComments(
        @Path("paperId") paperId: String,
        @Header("Authorization") token: String
    ): Response<List<CommentResponse>>

    @POST("api/comments")
    suspend fun addComment(
        @Body comment: CommentRequest,
        @Header("Authorization") token: String
    ): Response<CommentResponse>

    @POST("api/comments/counts")
    suspend fun getCommentCounts(
        @Body request: CommentCountRequest
    ): Response<Map<String, Int>>

    @POST("api/comments/{commentId}/vote")
    suspend fun voteComment(
        @Path("commentId") commentId: String,
        @Body vote: VoteRequest,
        @Header("Authorization") token: String
    ): Response<VoteResponse>

    @DELETE("api/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String,
        @Header("Authorization") token: String
    ): Response<DeleteResponse>
}

data class CommentResponse(
    val _id: String,
    val paperId: String,
    val content: String,
    val author: AuthorResponse,
    val parentId: String?,
    val votes: Int,
    val userVotes: List<UserVote>,
    val createdAt: String,
    val updatedAt: String,
    val replies: List<CommentResponse>? = null
)

data class AuthorResponse(
    val _id: String,
    val name: String,
    val profilePicture: String
)

data class UserVote(
    val user: String,
    val vote: Int
)

data class CommentRequest(
    val paperId: String,
    val content: String,
    val parentId: String? = null,
    val clientId: String? = null
)

data class VoteRequest(
    val vote: Int // 1 for upvote, -1 for downvote
)

data class VoteResponse(
    val votes: Int
)

data class DeleteResponse(
    val message: String
)

data class CommentCountRequest(
    val paperIds: List<String>
) 