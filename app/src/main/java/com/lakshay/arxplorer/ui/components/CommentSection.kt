package com.lakshay.arxplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.Duration
import java.time.ZonedDateTime
import androidx.compose.material3.LocalTextStyle

data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String,
    val content: String,
    val parentId: String?,
    val upvotes: List<String>,
    val downvotes: List<String>,
    val createdAt: ZonedDateTime,
    val replies: List<Comment> = emptyList()
)

@Composable
fun CommentSection(
    comments: List<Comment>,
    currentUserId: String,
    userPhotoUrl: String,
    onUpvote: (Comment) -> Unit,
    onDownvote: (Comment) -> Unit,
    onReply: (Comment, String) -> Unit,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val textColor = Color(0xFF000000)
    
    val rootComments = remember(comments) {
        comments.filter { it.parentId == null }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Comment input field
        CommentInput(
            userPhotoUrl = userPhotoUrl,
            onSubmit = onAddComment,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // Comments list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rootComments) { comment ->
                CommentWithReplies(
                    comment = comment,
                    replies = comment.replies,
                    currentUserId = currentUserId,
                    onUpvote = onUpvote,
                    onDownvote = onDownvote,
                    onReply = onReply
                )
            }
        }
    }
}

@Composable
fun CommentWithReplies(
    comment: Comment,
    replies: List<Comment>,
    currentUserId: String,
    onUpvote: (Comment) -> Unit,
    onDownvote: (Comment) -> Unit,
    onReply: (Comment, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplies by remember { mutableStateOf(false) }
    val deepPurple = Color(0xFF4A148C)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Main comment
        CommentItem(
            comment = comment,
            currentUserId = currentUserId,
            onUpvote = onUpvote,
            onDownvote = onDownvote,
            onReply = onReply,
            showReplyButton = true
        )

        // Show replies button if there are any
        if (replies.isNotEmpty()) {
            TextButton(
                onClick = { showReplies = !showReplies },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = deepPurple
                ),
                modifier = Modifier.padding(start = 28.dp, top = 0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (showReplies) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (showReplies) "Hide replies" else "Show replies",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showReplies) "Hide ${replies.size} ${if (replies.size == 1) "reply" else "replies"}" 
                           else "Show ${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Replies
        if (showReplies) {
            Column(
                modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                replies.forEach { reply ->
                    CommentItem(
                        comment = reply,
                        currentUserId = currentUserId,
                        onUpvote = onUpvote,
                        onDownvote = onDownvote,
                        onReply = onReply,
                        showReplyButton = false
                    )
                }
            }
        }
    }
}

@Composable
fun CommentInput(
    userPhotoUrl: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val textColor = Color(0xFF000000)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = userPhotoUrl,
            contentDescription = "User photo",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        TextField(
            value = commentText,
            onValueChange = { commentText = it },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            placeholder = { Text("Add a comment...", color = Color.Gray) },
            textStyle = LocalTextStyle.current.copy(color = textColor),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = lightPurple.copy(alpha = 0.1f),
                unfocusedContainerColor = lightPurple.copy(alpha = 0.1f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = deepPurple
            ),
            trailingIcon = {
                if (commentText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSubmit(commentText)
                            commentText = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = deepPurple
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String,
    onUpvote: (Comment) -> Unit,
    onDownvote: (Comment) -> Unit,
    onReply: (Comment, String) -> Unit,
    showReplyButton: Boolean,
    modifier: Modifier = Modifier
) {
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val textColor = Color(0xFF000000)
    var showReplyInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = comment.userPhotoUrl,
                contentDescription = "User photo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    Text(
                        text = "• ${getTimeAgo(comment.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Voting buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUpvote(comment) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (comment.upvotes.contains(currentUserId)) 
                                    Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Upvote",
                                tint = if (comment.upvotes.contains(currentUserId)) 
                                    deepPurple else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = comment.upvotes.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        IconButton(
                            onClick = { onDownvote(comment) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (comment.downvotes.contains(currentUserId)) 
                                    Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                contentDescription = "Downvote",
                                tint = if (comment.downvotes.contains(currentUserId)) 
                                    deepPurple else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = comment.downvotes.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Reply button - only show for top-level comments
                    if (showReplyButton) {
                        TextButton(
                            onClick = { showReplyInput = !showReplyInput },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = deepPurple
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Reply")
                        }
                    }
                }

                // Reply input field
                if (showReplyInput) {
                    CommentInput(
                        userPhotoUrl = comment.userPhotoUrl,
                        onSubmit = { text ->
                            onReply(comment, text)
                            showReplyInput = false
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getTimeAgo(dateTime: ZonedDateTime): String {
    val duration = Duration.between(dateTime, ZonedDateTime.now())
    return when {
        duration.toDays() > 365 -> "${duration.toDays() / 365}y"
        duration.toDays() > 30 -> "${duration.toDays() / 30}mo"
        duration.toDays() > 0 -> "${duration.toDays()}d"
        duration.toHours() > 0 -> "${duration.toHours()}h"
        duration.toMinutes() > 0 -> "${duration.toMinutes()}m"
        else -> "now"
    }
} 