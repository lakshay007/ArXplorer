package com.lakshay.arxplorer.ui

import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakshay.arxplorer.ui.home.HomeScreen
import com.lakshay.arxplorer.ui.home.HomeViewModel
import com.lakshay.arxplorer.ui.paper.PaperScreen
import com.lakshay.arxplorer.ui.paper.PaperViewModel
import com.lakshay.arxplorer.ui.preferences.PreferencesScreen
import com.lakshay.arxplorer.ui.preferences.PreferencesViewModel
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.ui.auth.AuthViewModel
import com.lakshay.arxplorer.ui.auth.AuthState

@Composable
fun ArXplorerApp(
    homeViewModel: HomeViewModel,
    paperViewModel: PaperViewModel,
    preferencesViewModel: PreferencesViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPaper by remember { mutableStateOf<ArxivPaper?>(null) }
    var showPreferences by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            selectedPaper != null -> {
                PaperScreen(
                    paper = selectedPaper!!,
                    onBackClick = { selectedPaper = null },
                    onDownloadClick = { paperViewModel.downloadPdf(it) },
                    onShareClick = { paperViewModel.sharePaper(it) }
                )
            }
            showPreferences -> {
                PreferencesScreen(
                    onPreferencesSelected = { preferences ->
                        preferencesViewModel.savePreferences(preferences) {
                            showPreferences = false
                            homeViewModel.refreshPapers()
                        }
                    }
                )
            }
            else -> {
                when (val currentAuthState = authState) {
                    is AuthState.Authenticated -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            username = currentAuthState.userName,
                            onPaperClick = { paper ->
                                selectedPaper = paper
                            },
                            onPreferencesNeeded = {
                                showPreferences = true
                            }
                        )
                    }
                    else -> {
                        // Handle other auth states if needed
                    }
                }
            }
        }
    }
} 