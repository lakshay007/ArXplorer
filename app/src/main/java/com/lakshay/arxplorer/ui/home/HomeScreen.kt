package com.lakshay.arxplorer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakshay.arxplorer.ui.common.UiState
import com.lakshay.arxplorer.ui.components.PaperCard
import com.lakshay.arxplorer.data.model.ArxivPaper
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
    val greeting = remember { getGreeting() }
    val uiState by viewModel.uiState.collectAsState()
    val showPreferences by viewModel.showPreferencesScreen.collectAsState()


    LaunchedEffect(showPreferences) {
        if (showPreferences) {
            onPreferencesNeeded()
        }
    }


    val lightPurple = Color(0xFFF3E5F5)
    val mediumPurple = Color(0xFFE1BEE7)
    val darkPurple = Color(0xFFCE93D8)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top section with gradient background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lightPurple,
                            mediumPurple,
                            lightPurple
                        )
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // Greeting and notifications row
            Row(
                modifier = Modifier
                    .fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 16.sp,
                        color = Color(0xFF6A1B9A).copy(alpha = 0.7f)
                    )
                    Text(
                        text = username,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A148C)
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
                        tint = Color(0xFF4A148C)
                    )
                }
            }
            
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* TODO: Implement search */ },
                active = false,
                onActiveChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF6A1B9A)
                    )
                },
                placeholder = {
                    Text(
                        text = "Search papers...",
                        color = Color(0xFF6A1B9A).copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                colors = SearchBarDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    dividerColor = Color.Transparent
                )
            ) {
                // Search suggestions will go here
            }
        }

        // Papers list with pull to refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No papers found for your preferences",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.data) { paper ->
                                PaperCard(
                                    paper = paper,
                                    onClick = { onPaperClick(paper) }
                                )
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.refreshPapers() }
                            ) {
                                Text("Retry")
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