package com.lakshay.arxplorer.ui.preferences

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onPreferencesSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val selectedPreferences = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF00BFA5)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(top = 27.dp)


            ) {
                Text(
                    text = "CHOOSE",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "INTERESTS & LANGUAGES",
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Content section with white background
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Search bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(28.dp)),
                        placeholder = { Text("Search topics...") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    // Categories grid
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories.filter {
                            searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                        }) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = category == selectedCategory,
                                selectedPreferences = selectedPreferences,
                                onCategoryClick = { 
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                onSubcategorySelected = { subcategory ->
                                    if (selectedPreferences.contains(subcategory)) {
                                        selectedPreferences.remove(subcategory)
                                    } else {
                                        selectedPreferences.add(subcategory)
                                    }
                                }
                            )
                        }
                    }

                    // Bottom buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Button(
                            onClick = { onPreferencesSelected(selectedPreferences.toList()) },
                            enabled = selectedPreferences.isNotEmpty(),
                            modifier = Modifier.width(120.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    selectedPreferences: List<String>,
    onCategoryClick: () -> Unit,
    onSubcategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
    ) {
        Surface(
            onClick = onCategoryClick,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                category.subcategories.forEach { subcategory ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPreferences.contains(subcategory),
                            onCheckedChange = { onSubcategorySelected(subcategory) }
                        )
                        Text(
                            text = subcategory,
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

data class Category(
    val name: String,
    val subcategories: List<String>
)

val categories = listOf(
    Category(
        name = "Computer Science",
        subcategories = listOf(
            "Artificial Intelligence",
            "Computation and Language",
            "Computer Vision and Pattern Recognition",
            "Cryptography and Security",
            "Databases",
            "Human-Computer Interaction",
            "Machine Learning",
            "Programming Languages",
            "Robotics",
            "Software Engineering"
        )
    ),
    Category(
        name = "Physics",
        subcategories = listOf(
            "Astrophysics",
            "Quantum Physics",
            "High Energy Physics",
            "Nuclear Physics",
            "Condensed Matter Physics",
            "Mathematical Physics",
            "Applied Physics",
            "Computational Physics"
        )
    ),
    Category(
        name = "Mathematics",
        subcategories = listOf(
            "Algebra",
            "Geometry",
            "Number Theory",
            "Analysis",
            "Probability",
            "Statistics",
            "Logic",
            "Combinatorics"
        )
    )
    // Add other categories as needed
) 