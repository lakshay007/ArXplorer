package com.lakshay.arxplorer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.repository.TimePeriod
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    username: String,
    onPaperClick: (ArxivPaper) -> Unit,
    onPreferencesNeeded: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSortOptions by remember { mutableStateOf(false) }
    var selectedSortBy by remember { mutableStateOf("all") }
    var selectedSortOrder by remember { mutableStateOf("descending") }
    var selectedFilter by rememberSaveable { mutableStateOf("new") }
    var showTopMenu by remember { mutableStateOf(false) }
    val greeting = remember { getGreeting() }
    val uiState by viewModel.uiState.collectAsState()
    val showPreferences by viewModel.showPreferencesScreen.collectAsState()
    val isLoading = uiState is UiState.Loading

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

    
    fun performSearch() {
        viewModel.searchPapers(
            query = searchQuery.trim(),
            sortBy = selectedSortBy,
            sortOrder = selectedSortOrder
        )
    }

    LaunchedEffect(showPreferences) {
        if (showPreferences) {
            onPreferencesNeeded()
        }
    }

    val deepPurple = Color(0xFF4A148C) // Primary color
    val lightPurple = Color(0xFFF3E5F5) // Surface color
    val mediumPurple = Color(0xFFE1BEE7) // Surface variant
    val darkPurple = Color(0xFF6A1B9A) // Secondary color

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightPurple)
    ) {
        // Top section with gradient background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            mediumPurple,
                            mediumPurple.copy(alpha = 0.7f),
                            lightPurple
                        )
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // Greeting and notifications row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 16.sp,
                        color = darkPurple.copy(alpha = 0.7f)
                    )
                    Text(
                        text = username,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = deepPurple
                    )
                }
                IconButton(
                    onClick = { /* TODO: Handle notifications */ },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = deepPurple
                    )
                }
            }
            
            // Search bar with sort button
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { performSearch() },
                active = false,
                onActiveChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = darkPurple
                    )
                },
                trailingIcon = {
                    Box {
                        IconButton(
                            onClick = { showSortOptions = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = lightPurple.copy(alpha = 0.7f),
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
                                            .background(darkPurple.copy(alpha = 0.8f))
                                    )
                                    // End circle
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(Color.White, RoundedCornerShape(2.dp))
                                            .border(1.dp, darkPurple.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
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
                                            .background(Color.White, RoundedCornerShape(2.dp))
                                            .border(1.dp, darkPurple.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                    // Line
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(2.dp)
                                            .background(darkPurple.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showSortOptions,
                            onDismissRequest = { showSortOptions = false },
                            modifier = Modifier
                                .background(color = Color.White.copy(alpha = 0.95f))
                                .width(200.dp)
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.titleSmall,
                                color = darkPurple,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            sortByOptions.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedSortBy = value
                                        showSortOptions = false
                                        performSearch()
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = darkPurple
                                    ),
                                    modifier = Modifier.background(
                                        if (selectedSortBy == value) lightPurple else Color.Transparent
                                    )
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = darkPurple.copy(alpha = 0.1f)
                            )

                            Text(
                                text = "Sort Order",
                                style = MaterialTheme.typography.titleSmall,
                                color = darkPurple,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            sortOrderOptions.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedSortOrder = value
                                        showSortOptions = false
                                        performSearch()
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = darkPurple
                                    ),
                                    modifier = Modifier.background(
                                        if (selectedSortOrder == value) lightPurple else Color.Transparent
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
                    containerColor = Color.White.copy(alpha = 0.9f),
                    dividerColor = Color.Transparent,
                    inputFieldColors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = darkPurple
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
                        if (!isLoading) {  // Prevent clicking while loading
                            selectedFilter = "new"
                            viewModel.refreshPapers()
                        }
                    },
                    label = { Text("New on ArXiv") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = deepPurple,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.9f),
                        labelColor = darkPurple
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
                                Text("Top published papers")
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Show time periods",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = deepPurple,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.9f),
                            labelColor = darkPurple
                        )
                    )

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.95f)
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
                                        color = darkPurple
                                    ) 
                                },
                                onClick = {
                                    selectedFilter = "top_${label.lowercase(Locale.getDefault()).replace(" ", "_")}"
                                    showTopMenu = false
                                    viewModel.loadTopPapers(period)
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = darkPurple,
                                    leadingIconColor = darkPurple
                                ),
                                modifier = Modifier.background(
                                    color = if (selectedFilter == "top_${label.lowercase(Locale.getDefault()).replace(" ", "_")}") 
                                        lightPurple else Color.Transparent
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
                .background(lightPurple)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = deepPurple)
                    }
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No papers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = darkPurple.copy(alpha = 0.6f)
                        )
                    }
                }
                is UiState.Success -> {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(false),
                        onRefresh = { viewModel.refreshPapers() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val isLoadingMore by viewModel.isLoadingMore.collectAsState()

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(state.data) { paper ->
                                PaperCard(
                                    paper = paper,
                                    onClick = { onPaperClick(paper) },
                                    modifier = Modifier.fillMaxWidth()
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
                                        Button(
                                            onClick = { viewModel.loadMorePapers() },
                                            enabled = !isLoadingMore,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = deepPurple,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth(0.5f)
                                        ) {
                                            if (isLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text("Load More")
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
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Red
                        )
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