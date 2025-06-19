package com.lakshay.arxplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lakshay.arxplorer.data.model.ArxivPaper
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakshay.arxplorer.viewmodel.ChatViewModel
import com.lakshay.arxplorer.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import com.lakshay.arxplorer.ui.theme.LocalAppColors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperCard(
    paper: ArxivPaper,
    onClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    commentCount: Int = 0,
    isBookmarked: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var showDialog by remember { mutableStateOf(false) }
    var isAiChat by remember { mutableStateOf(false) }
    var showSizeLimitDialog by remember { mutableStateOf(false) }
    var isCheckingSize by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val client = remember { 
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build() 
    }
    
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory.getInstance()
    )

    suspend fun getPaperSize(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            client.newCall(request).execute().use { response ->
                response.header("content-length")?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    if (showDialog) {
        AiPaperBottomSheet(
            paper = paper,
            isAiChat = isAiChat,
            viewModel = chatViewModel,
            onDismiss = { 
                showDialog = false
                chatViewModel.clearChat()
            }
        )
    }

    if (showSizeLimitDialog) {
        Dialog(onDismissRequest = { showSizeLimitDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.cardBackground
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Unlock Full Paper Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This paper is a bit too large for our free tier. We're working on a premium version that'll handle papers of any size! 🚀",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showSizeLimitDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary
                        )
                    ) {
                        Text("Cool, got it!")
                    }
                }
            }
        }
    }

    fun handleAiFeatureClick(isChat: Boolean) {
        isAiChat = isChat
        isCheckingSize = true
        
        coroutineScope.launch {
            val size = getPaperSize(paper.pdfUrl)
            isCheckingSize = false
            
            if (size > 10 * 1024 * 1024) {
                showSizeLimitDialog = true
            } else {
                showDialog = true
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.background
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Category and Date row at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip with subtle background
                Surface(
                    color = colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = paper.primaryCategory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = colors.primary
                    )
                }
                
                // Date text
                Text(
                    text = paper.publishedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title with increased prominence
            Text(
                text = paper.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 24.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Abstract with improved readability
            Text(
                text = paper.abstract,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp
                ),
                color = colors.textSecondary.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom row with comment count and AI buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCommentClick
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Comments",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = commentCount.toString(),
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                    
                    // Bookmark button - fixed alignment by replacing IconButton with Icon in a clickable box
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBookmarkClick
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // AI action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ask AI button
                    TextButton(
                        onClick = { handleAiFeatureClick(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ask AI about this",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Bottom separator with gradient effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to colors.surfaceVariant.copy(alpha = 0f),
                        0.1f to colors.surfaceVariant.copy(alpha = 0.5f),
                        0.9f to colors.surfaceVariant.copy(alpha = 0.5f),
                        1f to colors.surfaceVariant.copy(alpha = 0f)
                    )
                )
        )
    }
} 