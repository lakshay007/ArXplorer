package com.lakshay.arxplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperCard(
    paper: ArxivPaper,
    onClick: () -> Unit,
    onCommentClick: () -> Unit,
    commentCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val darkPurple = Color(0xFF6A1B9A)

    var showDialog by remember { mutableStateOf(false) }
    var isAiChat by remember { mutableStateOf(false) }
    var showSizeLimitDialog by remember { mutableStateOf(false) }
    var isCheckingSize by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    
    // Initialize ViewModel
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory.getInstance()
    )

    // Function to check paper size
    suspend fun getPaperSize(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()  // Use HEAD request to only get headers
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
                    containerColor = Color.White
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
                        tint = deepPurple,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Unlock Full Paper Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = deepPurple,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This paper is a bit too large for our free tier. We're working on a premium version that'll handle papers of any size! 🚀",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showSizeLimitDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = deepPurple
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
            
            if (size > 10 * 1024 * 1024) { // 10MB limit
                showSizeLimitDialog = true
            } else {
                showDialog = true
            }
        }
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = paper.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Authors
            Text(
                text = paper.authors.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Abstract
            Text(
                text = paper.abstract,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row with category chip and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = paper.primaryCategory,
                            color = Color.Black
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                            tint = deepPurple
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFF5F5F5),
                        labelColor = Color.Black,
                        leadingIconContentColor = deepPurple
                    )
                )

                // Date
                Text(
                    text = paper.publishedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Feature buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Prevent click propagation */ },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comment button
                FilledTonalButton(
                    onClick = { 
                        onCommentClick()
                    },
                    modifier = Modifier
                        .weight(0.3f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Prevent click propagation */ },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = lightPurple.copy(alpha = 0.5f),
                        contentColor = deepPurple.copy(alpha = 0.7f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(commentCount.toString())
                }

                // AI Chat button
                FilledTonalButton(
                    onClick = { 
                        handleAiFeatureClick(true)
                    },
                    enabled = !isCheckingSize,
                    modifier = Modifier
                        .weight(0.7f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isCheckingSize
                        ) { /* Prevent click propagation */ },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = deepPurple.copy(alpha = 0.1f),
                        contentColor = deepPurple,
                        disabledContainerColor = deepPurple.copy(alpha = 0.05f),
                        disabledContentColor = deepPurple.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (isCheckingSize) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = deepPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI about this")
                }

                // Summary button
                FilledTonalIconButton(
                    onClick = { 
                        handleAiFeatureClick(false)
                    },
                    enabled = !isCheckingSize,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isCheckingSize
                        ) { /* Prevent click propagation */ },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = lightPurple.copy(alpha = 0.5f),
                        contentColor = deepPurple.copy(alpha = 0.7f),
                        disabledContainerColor = lightPurple.copy(alpha = 0.3f),
                        disabledContentColor = deepPurple.copy(alpha = 0.3f)
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (isCheckingSize) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = deepPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "Summarize",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
} 