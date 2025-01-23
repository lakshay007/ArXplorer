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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lakshay.arxplorer.ui.auth.AuthState
import com.lakshay.arxplorer.ui.auth.AuthViewModel
import com.lakshay.arxplorer.ui.components.ErrorScreen
import com.lakshay.arxplorer.ui.components.LoadingScreen
import com.lakshay.arxplorer.ui.home.HomeScreen
import com.lakshay.arxplorer.ui.onboarding.OnboardingScreen
import com.lakshay.arxplorer.ui.theme.ArXplorerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check for existing Google Sign In account and sign out to ensure fresh login
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Previous sign-in state cleared")
        }

        setContent {
            ArXplorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by viewModel.authState.collectAsState()
                    Log.d(TAG, "Current auth state: $authState")

                    when (authState) {
                        is AuthState.Unauthenticated, is AuthState.Initial -> {
                            OnboardingScreen(
                                onSignInClick = { signIn() }
                            )
                        }
                        is AuthState.Authenticated -> {
                            val username = (authState as AuthState.Authenticated).userName
                            Log.d(TAG, "Authenticated user: $username")
                            HomeScreen(username = username)
                        }
                        is AuthState.Loading -> {
                            LoadingScreen()
                        }
                        is AuthState.Error -> {
                            val error = (authState as AuthState.Error).message
                            Log.e(TAG, "Auth error: $error")
                            ErrorScreen(
                                message = error,
                                onRetry = { 
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        signIn()
                                    }
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
                    viewModel.handleSignInResult(idToken)
                } else {
                    Log.e(TAG, "No ID token received")
                    viewModel.handleSignInResult("")
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
                viewModel.handleSignInResult("")
            }
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
} 