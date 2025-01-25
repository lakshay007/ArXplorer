package com.lakshay.arxplorer.ui.preferences

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "PreferencesViewModel"

class PreferencesViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _preferencesState = MutableStateFlow<PreferencesState>(PreferencesState.Loading)
    val preferencesState: StateFlow<PreferencesState> = _preferencesState

    init {
        checkUserPreferences()
    }

    fun checkUserPreferences() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "No authenticated user found")
                    _preferencesState.value = PreferencesState.Error("User not authenticated")
                    return@launch
                }
                
                Log.d(TAG, "Checking preferences for user: $userId")
                val preferences = firestore.collection("user_preferences")
                    .document(userId)
                    .get()
                    .await()

                if (preferences.exists()) {
                    val prefs = preferences.get("preferences") as? List<String>
                    Log.d(TAG, "Found existing preferences: $prefs")
                    _preferencesState.value = PreferencesState.PreferencesSet
                } else {
                    Log.d(TAG, "No preferences found, needs setup")
                    _preferencesState.value = PreferencesState.NeedsPreferences
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking preferences", e)
                _preferencesState.value = PreferencesState.Error(
                    when {
                        e.message?.contains("PERMISSION_DENIED") == true -> 
                            "Permission denied. Please check Firestore rules."
                        e.message?.contains("UNAUTHENTICATED") == true -> 
                            "User authentication required."
                        else -> e.message ?: "Unknown error"
                    }
                )
            }
        }
    }

    fun savePreferences(preferences: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e(TAG, "No authenticated user found while saving preferences")
                    _preferencesState.value = PreferencesState.Error("User not authenticated")
                    return@launch
                }

                Log.d(TAG, "Saving preferences for user: $userId")
                Log.d(TAG, "Preferences to save: $preferences")
                
                val preferencesMap = hashMapOf(
                    "preferences" to preferences,
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection("user_preferences")
                    .document(userId)
                    .set(preferencesMap)
                    .await()

                Log.d(TAG, "Successfully saved preferences")
                _preferencesState.value = PreferencesState.PreferencesSet
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving preferences", e)
                _preferencesState.value = PreferencesState.Error(
                    when {
                        e.message?.contains("PERMISSION_DENIED") == true -> 
                            "Permission denied. Please check Firestore rules."
                        e.message?.contains("UNAUTHENTICATED") == true -> 
                            "User authentication required."
                        else -> e.message ?: "Unknown error"
                    }
                )
            }
        }
    }
}

sealed class PreferencesState {
    object Loading : PreferencesState()
    object NeedsPreferences : PreferencesState()
    object PreferencesSet : PreferencesState()
    data class Error(val message: String) : PreferencesState()
} 