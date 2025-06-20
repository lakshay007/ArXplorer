package com.lakshay.arxplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.viewmodel.ChatViewModel
import com.lakshay.arxplorer.viewmodel.Message
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.lakshay.arxplorer.ui.theme.LocalAppColors
import com.lakshay.arxplorer.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AiPaperBottomSheet(
    paper: ArxivPaper,
    isAiChat: Boolean,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var userInput by remember { mutableStateOf("") }
    val chatState by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Prevent dismissal while loading
    LaunchedEffect(chatState.isLoading) {
        if (chatState.isLoading) {
            bottomSheetState.expand()
        }
    }

    // Auto-scroll to bottom when new messages arrive or when thinking state changes
    LaunchedEffect(chatState.messages.size, chatState.messages.lastOrNull()?.isThinking) {
        if (chatState.messages.isNotEmpty()) {
            listState.scrollToItem(chatState.messages.size - 1)
        }
    }

    // Start with summary if not in chat mode
    LaunchedEffect(Unit) {
        if (!isAiChat && chatState.messages.isEmpty()) {
            viewModel.summarizePaper(paper)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { 
            if (!chatState.isLoading) onDismiss() 
        },
        sheetState = bottomSheetState,
        dragHandle = { 
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = colors.background
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                }
            }
        },
        containerColor = colors.background,
        windowInsets = WindowInsets(0, 0, 0, 0),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAiChat) "Chat about Paper" else "Paper Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { if (!chatState.isLoading) onDismiss() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.primary
                        )
                    }
                }

                if (isAiChat) {
                    // Prompt chips
                    LazyRow(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(prompts) { prompt ->
                            SuggestionChip(
                                onClick = { userInput = prompt },
                                label = { Text(prompt) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = colors.surfaceVariant.copy(alpha = 0.3f),
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = if (isAiChat) 120.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false
                ) {
                    if (chatState.messages.isEmpty() && !chatState.isLoading) {
                        item {
                            Text(
                                text = if (isAiChat) {
                                    "Ask any questions about the paper. I'll help you understand it better!"
                                } else {
                                    "Generating summary..."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textSecondary
                            )
                        }
                    }

                    items(chatState.messages) { message ->
                        ChatMessage(
                            message = message,
                            colors = colors
                        )
                    }
                }
            }

            // Input field
            if (isAiChat) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = colors.background,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .navigationBarsPadding()
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your question here...", color = colors.textSecondary.copy(alpha = 0.6f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (userInput.isNotBlank()) {
                                        viewModel.sendMessage(paper, userInput)
                                        userInput = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            enabled = !chatState.isLoading
                        )
                        
                        IconButton(
                            onClick = {
                                if (userInput.isNotBlank()) {
                                    viewModel.sendMessage(paper, userInput)
                                    userInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userInput.isBlank() || chatState.isLoading) 
                                        colors.primary.copy(alpha = 0.5f) 
                                    else colors.primary
                                ),
                            enabled = userInput.isNotBlank() && !chatState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = colors.background,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessage(
    message: Message,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .align(if (message.isUser) Alignment.End else Alignment.Start)
                .widthIn(max = 300.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (message.isThinking) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "ArXai is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        message.isError -> colors.surfaceVariant.copy(alpha = 0.3f)
                        message.isUser -> colors.primary.copy(alpha = 0.1f)
                        else -> colors.surfaceVariant.copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            message.isError -> Color.Red
                            message.isUser -> colors.textPrimary
                            else -> colors.textPrimary
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private val prompts = listOf(
    "What are the key results?",
    "Summarize the paper",
    "What are the limitations?",
    "What is the main contribution?",
    "Explain the methodology",
    "Future work suggestions"
) 