package com.lakshay.arxplorer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseUser
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.ui.paper.PaperCommentsScreen
import com.lakshay.arxplorer.ui.home.HomeScreen
import com.lakshay.arxplorer.ui.home.HomeViewModel
import com.lakshay.arxplorer.ui.paper.PaperCommentsViewModel
import com.lakshay.arxplorer.ui.preferences.PreferencesScreen
import com.lakshay.arxplorer.ui.preferences.PreferencesViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    currentUser: FirebaseUser?,
    homeViewModel: HomeViewModel,
    username: String,
    onPaperClick: (ArxivPaper) -> Unit,
    onPreferencesNeeded: () -> Unit,
    viewModelFactory: PaperCommentsViewModel.Factory
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home Screen
        composable(route = "home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                username = username,
                onPaperClick = onPaperClick,
                onPreferencesNeeded = onPreferencesNeeded,
                navController = navController
            )
        }

        // Comments Screen
        composable(
            route = "comments/{paperId}",
            arguments = listOf(
                navArgument("paperId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val paperId = backStackEntry.arguments?.getString("paperId") ?: return@composable
            PaperCommentsScreen(
                paperId = paperId,
                currentUserId = currentUser?.uid ?: "",
                userPhotoUrl = currentUser?.photoUrl?.toString() ?: "",
                onBackClick = { navController.popBackStack() },
                viewModelFactory = viewModelFactory
            )
        }

        // Preferences Screen
        composable(
            route = "preferences?isFirstTime={isFirstTime}",
            arguments = listOf(
                navArgument("isFirstTime") {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { backStackEntry ->
            val isFirstTime = backStackEntry.arguments?.getBoolean("isFirstTime") ?: true
            val preferencesViewModel: PreferencesViewModel = hiltViewModel()
            
            PreferencesScreen(
                onPreferencesSelected = { selectedPreferences ->
                    preferencesViewModel.savePreferences(selectedPreferences) {
                        homeViewModel.refreshPapers()
                        navController.popBackStack()
                    }
                },
                isFirstTime = isFirstTime,
                onBackPressed = { navController.popBackStack() }
            )
        }

        // Add other screen routes here
    }
} 