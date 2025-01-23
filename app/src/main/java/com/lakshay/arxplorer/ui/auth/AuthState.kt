package com.lakshay.arxplorer.ui.auth

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userName: String) : AuthState()
    data class Error(val message: String) : AuthState()
} 