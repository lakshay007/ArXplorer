package com.lakshay.arxplorer.ui.preferences

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    onPreferencesSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    isFirstTime: Boolean = false,
    onBackPressed: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val selectedPreferences = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Load existing preferences when not first time
    LaunchedEffect(Unit) {
        if (!isFirstTime) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val preferencesDoc = Firebase.firestore
                        .collection("user_preferences")
                        .document(userId)
                        .get()
                        .await()

                    @Suppress("UNCHECKED_CAST")
                    val preferences = preferencesDoc.get("preferences") as? List<String>
                    preferences?.let { 
                        selectedPreferences.clear()
                        selectedPreferences.addAll(it)
                    }
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }

    // Filter categories and subcategories based on search
    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isEmpty()) {
            categories
        } else {
            categories.map { category ->
                val filteredSubcategories = category.subcategories.filter { subcategory ->
                    subcategory.contains(searchQuery, ignoreCase = true)
                }
                if (filteredSubcategories.isNotEmpty() || category.name.contains(searchQuery, ignoreCase = true)) {
                    category.copy(subcategories = filteredSubcategories)
                } else null
            }.filterNotNull()
        }
    }

    // Custom colors for modern UI
    val backgroundColor = Color(0xFFFAFAFA)
    val chipBackgroundColor = Color(0xFFF5F5F5)
    val selectedChipColor = Color(0xFFE8F3FF)
    val selectedChipTextColor = Color(0xFF1A73E8)
    val textColor = Color(0xFF202124)
    val subtextColor = Color(0xFF5F6368)

    // For dark mode elements
    val isDarkTheme = isSystemInDarkTheme()
    // Always keep text and icons dark for visibility
    val topBarTextColor = Color(0xFF202124)
    val topBarIconColor = Color(0xFF202124)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar with conditional back button
            if (!isFirstTime) {
                TopAppBar(
                    title = { 
                        Text(
                            "Edit Topics", 
                            color = topBarTextColor,
                            fontSize = 18.sp  // Smaller text size
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackPressed,
                            modifier = Modifier.size(40.dp)  // Smaller icon button
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarIconColor,
                                modifier = Modifier.size(20.dp)  // Smaller icon
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        navigationIconContentColor = topBarIconColor,
                        titleContentColor = topBarTextColor
                    ),
                    modifier = Modifier.height(48.dp)  // Reduced height
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = selectedChipTextColor)
                }
            } else {
                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)  // Changed to only horizontal padding
                        .padding(top = 8.dp)  // Added small top padding
                ) {
                    item {
                        // Header
                        Text(
                            text = "Choose Topics",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (selectedPreferences.isNotEmpty()) {
                            Text(
                                text = "Picked nice!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = selectedChipTextColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Text(
                            text = "${categories.sumOf { it.subcategories.size }} topics discovered",
                            fontSize = 16.sp,
                            color = subtextColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            placeholder = { Text("Search topics...", color = subtextColor) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = subtextColor
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = selectedChipTextColor,
                                cursorColor = selectedChipTextColor,
                                focusedContainerColor = chipBackgroundColor,
                                unfocusedContainerColor = chipBackgroundColor,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            singleLine = true
                        )

                        // Main categories
                        if (filteredCategories.isEmpty() && searchQuery.isNotEmpty()) {
                            Text(
                                text = "No topics found for \"$searchQuery\"",
                                fontSize = 16.sp,
                                color = subtextColor,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            filteredCategories.forEach { category ->
                                val isSelected = category == selectedCategory
                                Surface(
                                    onClick = { 
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) selectedChipColor else chipBackgroundColor,
                                    border = null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = selectedChipTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = category.name,
                                            color = if (isSelected) selectedChipTextColor else textColor,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected category's subcategories
                    item {
                        AnimatedVisibility(
                            visible = selectedCategory != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            selectedCategory?.let { category ->
                                Column {
                                    Text(
                                        text = "Topics in ${category.name}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        category.subcategories.forEach { subcategory ->
                                            val isSelected = selectedPreferences.contains(subcategory)
                                            Surface(
                                                onClick = { 
                                                    if (isSelected) {
                                                        selectedPreferences.remove(subcategory)
                                                    } else {
                                                        selectedPreferences.add(subcategory)
                                                    }
                                                },
                                                modifier = Modifier.height(40.dp),
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) selectedChipColor else chipBackgroundColor,
                                                border = null
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = selectedChipTextColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = subcategory,
                                                        color = if (isSelected) selectedChipTextColor else textColor,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected preferences summary
                    item {
                        if (selectedPreferences.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    text = "Selected (${selectedPreferences.size})",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    selectedPreferences.forEach { preference ->
                                        Surface(
                                            onClick = { selectedPreferences.remove(preference) },
                                            modifier = Modifier.height(40.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            color = selectedChipColor,
                                            border = null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = selectedChipTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = preference,
                                                    color = selectedChipTextColor,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = selectedChipTextColor,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom buttons in a fixed position
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = backgroundColor,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedPreferences.clear() },
                            enabled = selectedPreferences.isNotEmpty()
                        ) {
                            Text(
                                "Clear All",
                                color = if (selectedPreferences.isNotEmpty()) selectedChipTextColor else subtextColor
                            )
                        }
                        Button(
                            onClick = { onPreferencesSelected(selectedPreferences.toList()) },
                            enabled = selectedPreferences.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedChipTextColor,
                                disabledContainerColor = chipBackgroundColor
                            ),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Text(
                                "Apply (${selectedPreferences.size})",
                                color = if (selectedPreferences.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else subtextColor
                            )
                        }
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
    ),
    Category(
        name = "Economics",
        subcategories = listOf(
            "Econometrics",
            "General Economics",
            "Theoretical Economics"
        )
    ),
    Category(
        name = "Electrical Engineering and Systems Science",
        subcategories = listOf(
            "Audio and Speech Processing",
            "Image and Video Processing",
            "Signal Processing",
            "Systems and Control"
        )
    ),
    Category(
        name = "Quantitative Biology",
        subcategories = listOf(
            "Biomolecules",
            "Cell Behavior",
            "Genomics",
            "Molecular Networks",
            "Neurons and Cognition",
            "Other Quantitative Biology",
            "Populations and Evolution",
            "Quantitative Methods",
            "Subcellular Processes",
            "Tissues and Organs"
        )
    ),
    Category(
        name = "Statistics",
        subcategories = listOf(
            "Applications",
            "Computation",
            "Methodology",
            "Machine Learning",
            "Other Statistics",
            "Statistics Theory"
        )
    ),
    Category(
        name = "Quantitative Finance",
        subcategories = listOf(
            "Computational Finance",
            "Economics",
            "General Finance",
            "Mathematical Finance",
            "Portfolio Management",
            "Pricing of Securities",
            "Risk Management",
            "Statistical Finance",
            "Trading and Market Microstructure"
        )
    )
) 