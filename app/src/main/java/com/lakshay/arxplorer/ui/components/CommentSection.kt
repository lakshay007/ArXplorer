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
    val textColor = Color(0xFF000000) // Fixed dark text color
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Comment input field
        CommentInput(
            userPhotoUrl = userPhotoUrl,
            onSubmit = onAddComment,
            modifier = Modifier.padding(16.dp)
        )

        // Comments list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    currentUserId = currentUserId,
                    onUpvote = onUpvote,
                    onDownvote = onDownvote,
                    onReply = onReply,
                    depth = 0
                )
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
    val textColor = Color(0xFF000000) // Fixed dark text color

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
    depth: Int,
    modifier: Modifier = Modifier
) {
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val textColor = Color(0xFF000000) // Fixed dark text color
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (depth > 0) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(lightPurple)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = comment.userPhotoUrl,
                        contentDescription = "User photo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                    deepPurple else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = comment.upvotes.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                    deepPurple else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = comment.downvotes.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    TextButton(
                        onClick = { showReplyInput = !showReplyInput },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Reply",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reply")
                    }
                }

                if (showReplyInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            placeholder = { Text("Write a reply...", color = Color.Gray) },
                            textStyle = LocalTextStyle.current.copy(color = textColor),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = lightPurple.copy(alpha = 0.1f),
                                unfocusedContainerColor = lightPurple.copy(alpha = 0.1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = deepPurple
                            )
                        )
                        
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply(comment, replyText)
                                    replyText = ""
                                    showReplyInput = false
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (replyText.isBlank()) lightPurple else deepPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send reply",
                                tint = if (replyText.isBlank()) Color.Gray else Color.White
                            )
                        }
                    }
                }

                if (comment.replies.isNotEmpty()) {
                    comment.replies.forEach { reply ->
                        CommentItem(
                            comment = reply,
                            currentUserId = currentUserId,
                            onUpvote = onUpvote,
                            onDownvote = onDownvote,
                            onReply = onReply,
                            depth = depth + 1,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
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