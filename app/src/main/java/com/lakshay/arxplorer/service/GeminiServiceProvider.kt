package com.lakshay.arxplorer.service

object GeminiServiceProvider {
    private var instance: GeminiService? = null

    fun getInstance(): GeminiService {
        return instance ?: synchronized(this) {
            instance ?: GeminiService().also { instance = it }
        }
    }
} 