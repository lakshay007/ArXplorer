package com.lakshay.arxplorer.ui.auth

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userName: String) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
} 