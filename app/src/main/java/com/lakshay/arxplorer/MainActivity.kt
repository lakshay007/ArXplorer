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
import com.lakshay.arxplorer.ui.paper.PaperViewModel
import com.lakshay.arxplorer.ui.splash.SplashScreen
import com.lakshay.arxplorer.ui.ArXplorerApp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.navigation.compose.rememberNavController

private const val TAG = "MainActivity"
private const val RC_SIGN_IN = 9001

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
                val authState by authViewModel.authState.collectAsState()
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(authState) {
                    if (authState !is AuthState.Loading) {
                        delay(1000) // Show splash for at least 1 second
                        showSplash = false
                    }
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    when (authState) {
                        is AuthState.Loading -> {
                            // This case won't be visible due to splash screen
                        }
                        is AuthState.Authenticated -> {
                            ArXplorerApp(
                                homeViewModel = homeViewModel,
                                paperViewModel = paperViewModel,
                                preferencesViewModel = preferencesViewModel
                            )
                        }
                        is AuthState.Unauthenticated -> {
                            OnboardingScreen(
                                onSignInClick = {
                                    signIn()
                                }
                            )
                        }
                        is AuthState.Error -> {
                            ErrorScreen(
                                message = (authState as AuthState.Error).message,
                                onRetry = {
                                    signIn()
                                }
                            )
                        }
                        AuthState.Initial -> {
                            // This case won't be visible due to splash screen
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    Log.d(TAG, "Google sign in success")
                    authViewModel.handleSignInResult(token)
                } ?: run {
                    Log.e(TAG, "No ID token!")
                    authViewModel.handleSignInError("No ID token received")
                }
            } catch (e: ApiException) {
                val message = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign in failed"
                    else -> "Unknown error: ${e.statusCode}"
                }
                Log.e(TAG, "Google sign in failed: $message", e)
                authViewModel.handleSignInError(message)
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}

sealed class Screen {
    object Onboarding : Screen()
    object Home : Screen()
} 