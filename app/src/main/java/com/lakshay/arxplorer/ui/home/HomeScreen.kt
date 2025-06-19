package com.lakshay.arxplorer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.lakshay.arxplorer.ui.common.UiState
import com.lakshay.arxplorer.ui.components.PaperCard
import com.lakshay.arxplorer.ui.components.ThemeToggleButton
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.repository.TimePeriod
import java.util.*
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.lakshay.arxplorer.ui.theme.ThemeManager
import com.lakshay.arxplorer.ui.theme.LocalAppColors
import com.lakshay.arxplorer.ui.theme.ThemeViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    username: String,
    onPaperClick: (ArxivPaper) -> Unit,
    onPreferencesNeeded: () -> Unit,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSortOptions by remember { mutableStateOf(false) }
    var selectedSortBy by remember { mutableStateOf("all") }
    var selectedSortOrder by remember { mutableStateOf("descending") }
    var selectedFilter by rememberSaveable { mutableStateOf("new") }
    var showTopMenu by remember { mutableStateOf(false) }
    val greeting = remember { getGreeting() }
    val uiState by homeViewModel.uiState.collectAsState()
    val showPreferences by homeViewModel.showPreferencesScreen.collectAsState()
    val isLoading = uiState is UiState.Loading
    val isLoadingMore by homeViewModel.isLoadingMore.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val commentCounts by homeViewModel.commentCounts.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val bookmarkedPaperIds by homeViewModel.bookmarkedPaperIds.collectAsState()

    val sortByOptions = listOf(
        "All" to "all",
        "Title" to "title",
        "Relevance" to "relevance",
        "Last Updated" to "lastUpdatedDate",
        "Submitted Date" to "submittedDate"
    )
    val sortOrderOptions = listOf(
        "Descending" to "descending",
        "Ascending" to "ascending"
    )

    val colors = LocalAppColors.current
    val isDarkMode = ThemeManager.isDarkMode

    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

    fun performSearch() {
        homeViewModel.searchPapers(
            query = searchQuery,
            sortBy = selectedSortBy,
            sortOrder = selectedSortOrder
        )
        keyboardController?.hide()
    }

    LaunchedEffect(showPreferences) {
        if (showPreferences) {
            onPreferencesNeeded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top section with gradient background
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
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 8.dp)
        ) {
            // Greeting and dark mode toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 16.sp,
                        color = colors.secondary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = username,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bookmarks button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground.copy(alpha = 0.9f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { 
                                    navController.navigate("bookmarks") {
                                        launchSingleTop = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // Preferences/Interests button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground.copy(alpha = 0.9f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { 
                                    navController.navigate("preferences?isFirstTime=false") {
                                        launchSingleTop = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Research Interests",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    ThemeToggleButton(
                        isDarkTheme = isDarkTheme,
                        onToggle = { themeViewModel.toggleTheme() }
                    )
                }
            }
            
            // Search bar with sort button
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { 
                    performSearch()
                },
                active = false,
                onActiveChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.secondary
                    )
                },
                trailingIcon = {
                    Box {
                        IconButton(
                            onClick = { showSortOptions = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = colors.cardBackground.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // First line with end circle
                                Row(
                                    modifier = Modifier.width(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Line
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(2.dp)
                                            .background(colors.primary.copy(alpha = 0.8f))
                                    )
                                    // End circle
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(colors.cardBackground, RoundedCornerShape(2.dp))
                                            .border(1.dp, colors.primary.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(3.dp))
                                
                                // Second line with start circle
                                Row(
                                    modifier = Modifier.width(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start circle
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(colors.cardBackground, RoundedCornerShape(2.dp))
                                            .border(1.dp, colors.primary.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                    // Line
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(2.dp)
                                            .background(colors.primary.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showSortOptions,
                            onDismissRequest = { showSortOptions = false },
                            modifier = Modifier
                                .background(color = colors.cardBackground.copy(alpha = 0.95f))
                                .width(200.dp)
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            sortByOptions.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = label,
                                            color = if (selectedSortBy == value) colors.primary else colors.textPrimary
                                        ) 
                                    },
                                    onClick = {
                                        selectedSortBy = value
                                        showSortOptions = false
                                        performSearch()
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = colors.primary
                                    ),
                                    modifier = Modifier.background(
                                        if (selectedSortBy == value) colors.primary.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = colors.primary.copy(alpha = 0.1f)
                            )

                            Text(
                                text = "Sort Order",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            sortOrderOptions.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = label,
                                            color = if (selectedSortOrder == value) colors.primary else colors.textPrimary
                                        ) 
                                    },
                                    onClick = {
                                        selectedSortOrder = value
                                        showSortOptions = false
                                        performSearch()
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = colors.primary
                                    ),
                                    modifier = Modifier.background(
                                        if (selectedSortOrder == value) colors.primary.copy(alpha = 0.1f) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "Search papers...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = SearchBarDefaults.colors(
                    containerColor = colors.cardBackground.copy(alpha = 0.9f),
                    dividerColor = Color.Transparent,
                    inputFieldColors = TextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    )
                )
            ) {
                // Search suggestions will go here
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New chip
                FilterChip(
                    selected = selectedFilter == "new",
                    onClick = { 
                        if (!isLoading) {
                            selectedFilter = "new"
                            homeViewModel.loadPapers()
                        }
                    },
                    label = { Text("New on ArXiv") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.cardBackground,
                        containerColor = colors.cardBackground.copy(alpha = 0.9f),
                        labelColor = colors.secondary
                    )
                )

                // Top chip with dropdown
                Box {
                    FilterChip(
                        selected = selectedFilter.startsWith("top_", ignoreCase = true),
                        onClick = { 
                            if (!isLoading) {  // Prevent clicking while loading
                                showTopMenu = true 
                            }
                        },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Top cited papers")
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Show time periods",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.cardBackground,
                            containerColor = colors.cardBackground.copy(alpha = 0.9f),
                            labelColor = colors.secondary
                        )
                    )

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier
                            .background(
                                color = colors.cardBackground.copy(alpha = 0.95f)
                            )
                    ) {
                        val menuItems = listOf(
                            Pair("This Week", TimePeriod.THIS_WEEK),
                            Pair("This Month", TimePeriod.THIS_MONTH),
                            Pair("This Year", TimePeriod.THIS_YEAR),
                            Pair("All Time", TimePeriod.ALL_TIME)
                        )
                        
                        menuItems.forEach { (label, period) ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = label,
                                        color = colors.primary
                                    ) 
                                },
                                onClick = {
                                    selectedFilter = "top_${label.lowercase(Locale.getDefault()).replace(" ", "_")}"
                                    showTopMenu = false
                                    homeViewModel.loadTopPapers(period)
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = colors.primary,
                                    leadingIconColor = colors.primary
                                ),
                                modifier = Modifier.background(
                                    color = if (selectedFilter == "top_${label.lowercase(Locale.getDefault()).replace(" ", "_")}") 
                                        colors.cardBackground else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        // Papers list with pull to refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = "No papers found",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try adjusting your search filters or using different keywords",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val listState = rememberLazyListState()
                    
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing),
                        onRefresh = { 
                            homeViewModel.refreshPapers()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            contentPadding = PaddingValues(vertical = 0.dp)
                        ) {
                            items(
                                items = state.data,
                                key = { paper -> paper.id }  // Add key for stable item identity
                            ) { paper ->
                                val isBookmarked = bookmarkedPaperIds.contains(paper.id)
                                val commentCount = commentCounts[paper.id] ?: 0
                                
                                PaperCard(
                                    paper = paper,
                                    onClick = { onPaperClick(paper) },
                                    onCommentClick = { 
                                        homeViewModel.setCurrentPaper(paper)
                                        navController.navigate("paper/${paper.id}/comments")
                                    },
                                    commentCount = commentCount,
                                    isBookmarked = isBookmarked,
                                    onBookmarkClick = { homeViewModel.toggleBookmark(paper.id) }
                                )
                            }

                            item {
                                if (state.data.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            onClick = { homeViewModel.loadMorePapers() },
                                            enabled = !isLoadingMore,
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colors.primary,
                                                disabledContentColor = colors.primary.copy(alpha = 0.6f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isLoadingMore) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = colors.primary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Loading more papers...",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                } else {
                                                    Text(
                                                        "Load more papers",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = "Oops! Something went wrong",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please try refreshing or adjusting your search",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (state.message.contains("mark/reset")) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { homeViewModel.refreshPapers() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary
                                    )
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
} 