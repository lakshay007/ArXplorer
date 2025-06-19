package com.lakshay.arxplorer.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.ui.common.UiState
import com.lakshay.arxplorer.ui.components.PaperCard
import com.lakshay.arxplorer.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onBackClick: () -> Unit,
    onPaperClick: (ArxivPaper) -> Unit,
    onCommentClick: (ArxivPaper) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookmarkedPapers by viewModel.bookmarkedPapers.collectAsState()
    val commentCounts by viewModel.commentCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    val colors = LocalAppColors.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top bar with gradient background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surfaceVariant,
                            colors.surfaceVariant.copy(alpha = 0.7f),
                            colors.surface
                        )
                    )
                )
                .padding(top = 48.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bookmarks",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            }
        }
        
        // Paper list with swipe to refresh
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refreshBookmarks() },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                
                is UiState.Success -> {
                    if (bookmarkedPapers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = colors.textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No bookmarks yet",
                                    fontSize = 18.sp,
                                    color = colors.textSecondary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Bookmarked papers will appear here",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(bookmarkedPapers) { paper ->
                                PaperCard(
                                    paper = paper,
                                    onClick = { onPaperClick(paper) },
                                    onCommentClick = { onCommentClick(paper) },
                                    commentCount = commentCounts[paper.id] ?: 0,
                                    isBookmarked = true,
                                    onBookmarkClick = { viewModel.removeBookmark(paper.id) }
                                )
                            }
                        }
                    }
                }
                
                is UiState.Error -> {
                    val error = (uiState as UiState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No bookmarks yet",
                                fontSize = 18.sp,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bookmarked papers will appear here",
                                fontSize = 14.sp,
                                color = colors.textSecondary.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
} 