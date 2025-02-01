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
import com.lakshay.arxplorer.ui.theme.LocalAppColors

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
    val colors = LocalAppColors.current
    
    val rootComments = remember(comments) {
        comments.filter { it.parentId == null }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardBackground)
        ) {
            // Comment input field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.cardBackground
            ) {
                CommentInput(
                    userPhotoUrl = userPhotoUrl,
                    onSubmit = onAddComment,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                )
            }

            // Comments list
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.cardBackground
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rootComments) { comment ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.cardBackground
                        ) {
                            CommentWithReplies(
                                comment = comment,
                                replies = comment.replies,
                                currentUserId = currentUserId,
                                userPhotoUrl = userPhotoUrl,
                                onUpvote = onUpvote,
                                onDownvote = onDownvote,
                                onReply = onReply
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentWithReplies(
    comment: Comment,
    replies: List<Comment>,
    currentUserId: String,
    userPhotoUrl: String,
    onUpvote: (Comment) -> Unit,
    onDownvote: (Comment) -> Unit,
    onReply: (Comment, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplies by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Main comment
            CommentItem(
                comment = comment,
                currentUserId = currentUserId,
                userPhotoUrl = userPhotoUrl,
                onUpvote = onUpvote,
                onDownvote = onDownvote,
                onReply = onReply,
                showReplyButton = true
            )

            // Show replies button if there are any
            if (replies.isNotEmpty()) {
                Surface(
                    modifier = Modifier.offset(y = (-8).dp),
                    color = colors.cardBackground
                ) {
                    TextButton(
                        onClick = { showReplies = !showReplies },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary,
                            containerColor = colors.cardBackground
                        ),
                        modifier = Modifier.padding(start = 20.dp),
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
            }

            // Replies
            if (showReplies) {
                Surface(
                    modifier = Modifier
                        .padding(start = 28.dp)
                        .offset(y = (-4).dp),
                    color = colors.cardBackground
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        replies.forEach { reply ->
                            CommentItem(
                                comment = reply,
                                currentUserId = currentUserId,
                                userPhotoUrl = userPhotoUrl,
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
    }
}

@Composable
fun CommentInput(
    userPhotoUrl: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val colors = LocalAppColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = userPhotoUrl,
                contentDescription = "User photo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = { Text("Add a comment...", color = colors.textSecondary.copy(alpha = 0.6f)) },
                textStyle = LocalTextStyle.current.copy(color = colors.textPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.cardBackground,
                    unfocusedContainerColor = colors.cardBackground,
                    disabledContainerColor = colors.cardBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary
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
                                tint = colors.primary
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String,
    userPhotoUrl: String,
    onUpvote: (Comment) -> Unit,
    onDownvote: (Comment) -> Unit,
    onReply: (Comment, String) -> Unit,
    showReplyButton: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var showReplyInput by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Comment header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = comment.userPhotoUrl,
                    contentDescription = "User photo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    
                    Text(
                        text = getTimeAgo(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            // Comment content
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .padding(start = 32.dp, top = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voting buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upvote button with count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (comment.upvotes.contains(currentUserId)) 
                                    colors.surfaceVariant.copy(alpha = 0.3f)
                                else colors.cardBackground
                            )
                    ) {
                        IconButton(
                            onClick = { onUpvote(comment) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (comment.upvotes.contains(currentUserId)) 
                                    Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Upvote",
                                tint = if (comment.upvotes.contains(currentUserId)) 
                                    colors.primary else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = comment.upvotes.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (comment.upvotes.contains(currentUserId)) 
                                colors.primary else colors.textSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Downvote button with count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (comment.downvotes.contains(currentUserId)) 
                                    colors.surfaceVariant.copy(alpha = 0.3f)
                                else colors.cardBackground
                            )
                    ) {
                        IconButton(
                            onClick = { onDownvote(comment) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (comment.downvotes.contains(currentUserId)) 
                                    Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                contentDescription = "Downvote",
                                tint = if (comment.downvotes.contains(currentUserId)) 
                                    colors.primary else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = comment.downvotes.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (comment.downvotes.contains(currentUserId)) 
                                colors.primary else colors.textSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                if (showReplyButton) {
                    TextButton(
                        onClick = { showReplyInput = !showReplyInput },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reply", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Reply input
            if (showReplyInput) {
                CommentInput(
                    userPhotoUrl = userPhotoUrl,
                    onSubmit = { content ->
                        onReply(comment, content)
                        showReplyInput = false
                    },
                    modifier = Modifier.padding(start = 32.dp, top = 8.dp)
                )
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