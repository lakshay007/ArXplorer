package com.lakshay.arxplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lakshay.arxplorer.data.model.ArxivPaper
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakshay.arxplorer.viewmodel.ChatViewModel
import com.lakshay.arxplorer.viewmodel.ChatViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperCard(
    paper: ArxivPaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deepPurple = Color(0xFF4A148C)
    val lightPurple = Color(0xFFF3E5F5)
    val darkPurple = Color(0xFF6A1B9A)

    var showDialog by remember { mutableStateOf(false) }
    var isAiChat by remember { mutableStateOf(false) }
    
    // Initialize ViewModel
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory.getInstance()
    )

    if (showDialog) {
        AiPaperBottomSheet(
            paper = paper,
            isAiChat = isAiChat,
            viewModel = chatViewModel,
            onDismiss = { 
                showDialog = false
                // Clear chat when dialog is dismissed
                chatViewModel.clearChat()
            }
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Summary button
                FilledTonalButton(
                    onClick = { 
                        isAiChat = false
                        showDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = deepPurple.copy(alpha = 0.1f),
                        contentColor = deepPurple
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Summarize")
                }

                // AI Chat button
                FilledTonalIconButton(
                    onClick = { 
                        isAiChat = true
                        showDialog = true
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = darkPurple.copy(alpha = 0.1f),
                        contentColor = darkPurple
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Chat with AI",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
} 