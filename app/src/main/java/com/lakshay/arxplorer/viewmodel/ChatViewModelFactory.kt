package com.lakshay.arxplorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lakshay.arxplorer.service.GeminiServiceProvider

class ChatViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(GeminiServiceProvider.getInstance()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    companion object {
        private var instance: ChatViewModelFactory? = null

        fun getInstance(): ChatViewModelFactory {
            return instance ?: synchronized(this) {
                instance ?: ChatViewModelFactory().also { instance = it }
            }
        }
    }
} 