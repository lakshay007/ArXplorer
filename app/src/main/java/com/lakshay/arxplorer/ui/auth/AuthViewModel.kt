package com.lakshay.arxplorer.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthViewModel"

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()

        
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed: ${user?.displayName}")
            if (user != null) {
               
                viewModelScope.launch {
                    try {
                        user.getIdToken(true).await()
                        _authState.value = AuthState.Authenticated(user.displayName ?: "User")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing token", e)
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        Log.d(TAG, "Checking auth state: ${currentUser?.displayName}")
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                 
                    currentUser.getIdToken(true).await()
                    _authState.value = AuthState.Authenticated(currentUser.displayName ?: "User")
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing token", e)
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun handleSignInResult(idToken: String) {
        viewModelScope.launch {
            try {
                if (idToken.isEmpty()) {
                    throw IllegalArgumentException("ID Token cannot be empty")
                }
                
                _authState.value = AuthState.Loading
                Log.d(TAG, "Starting Firebase auth with token")
                
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                
                val user = result.user
                if (user != null) {
                    Log.d(TAG, "Firebase auth successful: ${user.displayName}")
                    _authState.value = AuthState.Authenticated(user.displayName ?: "User")
                } else {
                    Log.e(TAG, "Firebase auth failed: No user")
                    _authState.value = AuthState.Error("Authentication failed")
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid token", e)
                _authState.value = AuthState.Error("Invalid authentication token")
            } catch (e: Exception) {
                Log.e(TAG, "Auth error", e)
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun handleSignInError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener { checkAuthState() }
    }
} 