package com.lakshay.arxplorer.ui

import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.lakshay.arxplorer.navigation.NavGraph
import com.lakshay.arxplorer.ui.home.HomeViewModel
import com.lakshay.arxplorer.ui.paper.PaperViewModel
import com.lakshay.arxplorer.ui.paper.PaperScreen
import com.lakshay.arxplorer.ui.preferences.PreferencesViewModel
import com.lakshay.arxplorer.ui.preferences.PreferencesScreen
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.ui.auth.AuthViewModel
import com.lakshay.arxplorer.ui.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.lakshay.arxplorer.ui.paper.PaperCommentsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.lakshay.arxplorer.ui.theme.ThemeManager
import com.lakshay.arxplorer.ui.theme.LocalAppColors
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ArXplorerApp(
    homeViewModel: HomeViewModel,
    paperViewModel: PaperViewModel,
    preferencesViewModel: PreferencesViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentPaper by paperViewModel.currentPaper.collectAsState()
    var showPreferences by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    CompositionLocalProvider(LocalAppColors provides colors) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.background
        ) {
            NavGraph(
                navController = navController,
                startDestination = "home",
                currentUser = currentUser,
                homeViewModel = homeViewModel,
                username = currentUser?.displayName ?: "User",
                onPaperClick = { paper ->
                    paperViewModel.setPaper(paper)
                },
                onPreferencesNeeded = {
                    showPreferences = true
                },
                viewModelFactory = mainViewModel.commentsViewModelFactory
            )
        }

        // Show paper screen or preferences screen as overlays if needed
        currentPaper?.let { paper ->
            PaperScreen(
                onBackClick = { paperViewModel.setPaper(null) },
                onDownloadClick = { paperViewModel.downloadPdf(it) },
                onShareClick = { paperViewModel.sharePaper(it) }
            )
        }
        
        if (showPreferences) {
            PreferencesScreen(
                onPreferencesSelected = { selectedPreferences: List<String> ->
                    preferencesViewModel.savePreferences(selectedPreferences) {
                        showPreferences = false
                        homeViewModel.refreshPapers()
                    }
                }
            )
        }
    }
} 