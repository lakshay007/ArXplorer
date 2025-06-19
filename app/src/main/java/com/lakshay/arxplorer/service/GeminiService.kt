package com.lakshay.arxplorer.service

import android.util.Log
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiService"
private const val SERVER_URL = "https://arxplorerbackend.onrender.com/process-pdf" // Updated to hosted backend

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased timeout for Render's cold starts
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private suspend fun makeGeminiRequest(pdfUrl: String, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("pdfUrl", pdfUrl)
                put("prompt", prompt)
            }

            Log.d(TAG, "Making request to server: $SERVER_URL")
            Log.d(TAG, "Request body: ${jsonBody.toString()}")

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "Making request to server with PDF URL: $pdfUrl")
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Server error: ${response.code}, Body: $errorBody")
                throw IOException("Server returned error: ${response.code}")
            }
            
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            Log.d(TAG, "Raw response: $responseBody")
            

            val jsonResponse = JSONObject(responseBody)
            jsonResponse.getString("response")

        } catch (e: Exception) {
            Log.e(TAG, "Error making Gemini request", e)
            throw e
        }
    }

    suspend fun chat(paper: ArxivPaper, userInput: String): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting chat for paper: ${paper.title}")
            Log.d(TAG, "User input: $userInput")
            Log.d(TAG, "PDF URL: ${paper.pdfUrl}")


            emit("ArXAI is thinking...")

            val prompt = buildString {
                append("You are a helpful AI research assistant. ")
                append("I will provide you with a research paper, and I want you to help answer questions about it.\n\n")
                append("User Question: $userInput\n\n")
                append("Please provide a clear, concise, and accurate response based on the paper's content.")
            }

            Log.d(TAG, "Sending request to server")
            val response = makeGeminiRequest(paper.pdfUrl, prompt)
            Log.d(TAG, "Received response from server")
            emit(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error in chat", e)
            emit("Error: ${e.localizedMessage ?: "Something went wrong. Please try again."}")
        }
    }

    suspend fun summarize(paper: ArxivPaper): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting summary for paper: ${paper.title}")
            Log.d(TAG, "PDF URL: ${paper.pdfUrl}")

            // Emit thinking message
            emit("ArXAI is analyzing the paper...")

            val prompt = buildString {
                append("You are a helpful AI research assistant. ")
                append("I will provide you with a research paper, and I want you to provide a comprehensive summary of it.\n\n")
                append("Please provide a clear and structured summary of the paper, highlighting:\n")
                append("1. Main contributions and findings\n")
                append("2. Key methodology\n")
                append("3. Important results\n")
                append("4. Potential impact and applications")
            }

            Log.d(TAG, "Sending request to server")
            val response = makeGeminiRequest(paper.pdfUrl, prompt)
            Log.d(TAG, "Received response from server")
            emit(response)

        } catch (e: Exception) {
            Log.e(TAG, "Error in summarize", e)
            emit("Error: ${e.localizedMessage ?: "Something went wrong. Please try again."}")
        }
    }
} 