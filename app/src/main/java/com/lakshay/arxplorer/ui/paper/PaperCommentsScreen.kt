package com.lakshay.arxplorer.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.lakshay.arxplorer.ui.components.CommentSection
import com.lakshay.arxplorer.data.repository.ArxivRepository
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.lakshay.arxplorer.ui.theme.LocalAppColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperCommentsScreen(
    paperId: String,
    currentUserId: String,
    userPhotoUrl: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModelFactory: PaperCommentsViewModel.Factory,
) {
    val viewModel: PaperCommentsViewModel = viewModel(
        factory = PaperCommentsViewModel.provideFactory(viewModelFactory, paperId)
    )
    
    val colors = LocalAppColors.current
    val paper by viewModel.paper.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val error by viewModel.error.collectAsState()
    var isAbstractExpanded by remember { mutableStateOf(false) }

    // Show error if any
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You can show a snackbar or toast here
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
        ) {
            // Paper details card
            paper?.let { paperData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = paperData.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = paperData.publishedDate.format(
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isAbstractExpanded) {
                                        Modifier.heightIn(max = 400.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .then(
                                        if (isAbstractExpanded) {
                                            Modifier.verticalScroll(rememberScrollState())
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Text(
                                    text = paperData.abstract,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                    maxLines = if (isAbstractExpanded) Int.MAX_VALUE else 3
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = { isAbstractExpanded = !isAbstractExpanded },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colors.primary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (isAbstractExpanded) "Show less" else "Read more...")
                        }
                    }
                }
            }

            // Comments section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.cardBackground
                )
            ) {
                CommentSection(
                    comments = comments,
                    currentUserId = currentUserId,
                    userPhotoUrl = userPhotoUrl,
                    onUpvote = { comment -> 
                        viewModel.toggleUpvote(comment.id, currentUserId)
                    },
                    onDownvote = { comment ->
                        viewModel.toggleDownvote(comment.id, currentUserId)
                    },
                    onReply = { comment, replyText ->
                        viewModel.addReply(
                            parentCommentId = comment.id,
                            content = replyText,
                            userId = currentUserId,
                            userName = "User", // TODO: Get actual user name
                            userPhotoUrl = userPhotoUrl
                        )
                    },
                    onAddComment = { commentText ->
                        viewModel.addComment(
                            content = commentText,
                            userId = currentUserId,
                            userName = "User", // TODO: Get actual user name
                            userPhotoUrl = userPhotoUrl
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
} 