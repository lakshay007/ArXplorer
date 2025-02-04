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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.navigation.compose.rememberNavController
import com.lakshay.arxplorer.ui.paper.PaperCommentsViewModel
import javax.inject.Inject
import com.lakshay.arxplorer.ui.theme.ThemeViewModel
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavHostController
import com.lakshay.arxplorer.ui.common.UiState

private const val TAG = "MainActivity"
private const val RC_SIGN_IN = 9001

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val paperViewModel: PaperViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var navController: NavHostController? = null

    @Inject
    lateinit var viewModelFactory: PaperCommentsViewModel.Factory

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Add back press handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // First check if PDF viewer is open
                if (paperViewModel.currentPaper.value != null) {
                    paperViewModel.setPaper(null)
                    return
                }

                // Then handle navigation stack
                navController?.let { navController ->
                    if (navController.currentBackStackEntry?.destination?.route?.startsWith("comments/") == true) {
                        navController.popBackStack()
                    } else if (navController.previousBackStackEntry == null) {
                        finish()
                    } else {
                        navController.popBackStack()
                    }
                } ?: finish()
            }
        })

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            val navController = rememberNavController().also { navController = it }
            val authState by authViewModel.authState.collectAsState()
            var showSplash by remember { mutableStateOf(true) }
            
            // Initialize data and check auth state
            LaunchedEffect(Unit) {
                try {
                    // Wait for auth state to be determined
                    while (authState == AuthState.Initial || authState == AuthState.Loading) {
                        delay(100)
                    }
                    
                    delay(500) // Minimum splash screen duration
                    showSplash = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error during initialization", e)
                    showSplash = false
                }
            }
            
            ArXplorerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                ArXplorerApp(
                                    homeViewModel = homeViewModel,
                                    paperViewModel = paperViewModel,
                                    preferencesViewModel = preferencesViewModel,
                                    navController = navController
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
                            else -> {
                                // This shouldn't happen as we wait for initialization
                                LoadingScreen()
                            }
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