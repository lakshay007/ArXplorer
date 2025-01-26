package com.lakshay.arxplorer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import com.lakshay.arxplorer.ui.auth.AuthState
import com.lakshay.arxplorer.ui.auth.AuthViewModel
import com.lakshay.arxplorer.ui.components.ErrorScreen
import com.lakshay.arxplorer.ui.components.LoadingScreen
import com.lakshay.arxplorer.ui.home.HomeScreen
import com.lakshay.arxplorer.ui.home.HomeViewModel
import com.lakshay.arxplorer.ui.onboarding.OnboardingScreen
import com.lakshay.arxplorer.ui.theme.ArXplorerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.lakshay.arxplorer.ui.preferences.PreferencesScreen
import com.lakshay.arxplorer.ui.preferences.PreferencesViewModel
import com.lakshay.arxplorer.ui.preferences.PreferencesState
import com.lakshay.arxplorer.ui.paper.PaperScreen
import com.lakshay.arxplorer.ui.paper.PaperViewModel
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val paperViewModel: PaperViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check for existing signed-in user and attempt silent sign-in
        lifecycleScope.launch {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                if (account != null) {
                    // Try silent sign-in to refresh token
                    val silentSignInResult = googleSignInClient.silentSignIn().await()
                    val idToken = silentSignInResult.idToken
                    if (idToken != null) {
                        Log.d(TAG, "Silent sign-in successful")
                        authViewModel.handleSignInResult(idToken)
                    } else {
                        Log.d(TAG, "Silent sign-in failed - no token")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Silent sign-in failed", e)
                // Don't show error to user, they can still manually sign in
            }
        }

        setContent {
            ArXplorerTheme {
                var selectedPaper by remember { mutableStateOf<ArxivPaper?>(null) }
                var showPreferences by remember { mutableStateOf(false) }
                val authState by authViewModel.authState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
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
                                    Log.d(TAG, "Preferences selected: $preferences")
                                    preferencesViewModel.savePreferences(preferences) {
                                        Log.d(TAG, "Preferences saved, returning to home")
                                        showPreferences = false
                                        homeViewModel.refreshPapers()
                                    }
                                }
                            )
                        }
                        authState is AuthState.Authenticated -> {
                            val username = (authState as AuthState.Authenticated).userName
                            HomeScreen(
                                viewModel = homeViewModel,
                                username = username,
                                onPaperClick = { paper ->
                                    selectedPaper = paper
                                },
                                onPreferencesNeeded = {
                                    showPreferences = true
                                }
                            )
                        }
                        else -> {
                            OnboardingScreen(
                                onSignInClick = {
                                    signIn()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun signIn() {
        Log.d(TAG, "Starting sign in process")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Sign in result received")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google sign in success: ${account?.email}, ID Token: ${account?.idToken?.take(10)}...")
                val idToken = account?.idToken
                if (idToken != null) {
                    Log.d(TAG, "Got ID token, authenticating with Firebase")
                    authViewModel.handleSignInResult(idToken)
                } else {
                    Log.e(TAG, "No ID token received")
                    authViewModel.handleSignInResult("")
                }
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign in failed"
                    GoogleSignInStatusCodes.DEVELOPER_ERROR -> "Developer error - check OAuth configuration"
                    else -> "Unknown error: ${e.statusCode}"
                }
                Log.e(TAG, "Google sign in failed: $errorMessage", e)
                authViewModel.handleSignInResult("")
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}

sealed class Screen {
    object Onboarding : Screen()
    object Home : Screen()
} 