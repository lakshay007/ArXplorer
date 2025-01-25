package com.lakshay.arxplorer.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.ZonedDateTime

private const val TAG = "SemanticScholarApi"
private const val BASE_URL = "https://api.semanticscholar.org/graph/v1"
private const val API_KEY = "e0c6c9c9e7e3c0f0b9f0e0c6c9c9e7e3" // Replace with your actual API key

class SemanticScholarApi {
    private val client = OkHttpClient()

    suspend fun getCitationCount(paperId: String, doi: String?): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Try with DOI first if available
                if (doi != null) {
                    val doiCount = getCitationCountByDoi(doi)
                    if (doiCount >= 0) return@withContext doiCount
                }

                // Fallback to arXiv ID
                val arxivId = paperId.removePrefix("http://arxiv.org/abs/")
                getCitationCountByArxivId(arxivId)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching citation count for paper: $paperId", e)
                0
            }
        }
    }

    // New method to get top cited papers by field
    suspend fun getTopPapersByField(
        field: String,
        fromDate: String? = null,
        untilDate: String? = null,
        limit: Int = 20
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            // Convert field to appropriate query
            val query = when {
                field.startsWith("cs.") -> field.substring(3).replace(".", " ")
                field.startsWith("math.") -> field.substring(5).replace(".", " ")
                else -> field.replace(".", " ")
            }

            // Build date filter
            val dateFilter = if (fromDate != null && untilDate != null) {
                "&publicationDateOrYear=$fromDate:$untilDate"
            } else if (untilDate != null) {
                "&publicationDateOrYear=:$untilDate"
            } else ""

            // Build URL with all necessary parameters
            val url = "$BASE_URL/paper/search?query=\"$query\"" +
                     "&fields=title,citationCount,externalIds,publicationDate" +
                     dateFilter +
                     "&sort=citationCount:desc" +
                     "&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ArXplorer/1.0")
                .build()

            Log.d(TAG, "Making Semantic Scholar request: $url")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Error searching papers: ${response.code}")
                Log.e(TAG, "Response body: $responseBody")
                return@withContext emptyList()
            }

            val jsonResponse = JSONObject(responseBody ?: return@withContext emptyList())
            val papers = jsonResponse.getJSONArray("data")
            Log.d(TAG, "Found ${papers.length()} papers in total")

            val arxivIds = mutableListOf<String>()
            for (i in 0 until papers.length()) {
                val paper = papers.getJSONObject(i)
                val citationCount = paper.optInt("citationCount", 0)
                val externalIds = paper.optJSONObject("externalIds")
                val publicationDate = paper.optString("publicationDate", "")
                
                if (externalIds != null && externalIds.has("ArXiv")) {
                    val arxivId = externalIds.getString("ArXiv")
                    Log.d(TAG, "Found paper with arXiv ID: $arxivId, citations: $citationCount, date: $publicationDate")
                    arxivIds.add(arxivId)
                }
            }

            Log.d(TAG, "Found ${arxivIds.size} arXiv papers from Semantic Scholar")
            arxivIds
        } catch (e: Exception) {
            Log.e(TAG, "Error searching top papers: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    private fun getCitationCountByDoi(doi: String): Int {
        val request = Request.Builder()
            .url("$BASE_URL/paper/DOI:$doi?fields=citationCount")
            .addHeader("x-api-key", API_KEY)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return -1

            val jsonResponse = JSONObject(response.body?.string() ?: return -1)
            jsonResponse.optInt("citationCount", -1)
        } catch (e: IOException) {
            Log.e(TAG, "Error fetching citation count by DOI: $doi", e)
            -1
        }
    }

    private fun getCitationCountByArxivId(arxivId: String): Int {
        val request = Request.Builder()
            .url("$BASE_URL/paper/arXiv:$arxivId?fields=citationCount")
            .addHeader("x-api-key", API_KEY)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return 0

            val jsonResponse = JSONObject(response.body?.string() ?: return 0)
            jsonResponse.optInt("citationCount", 0)
        } catch (e: IOException) {
            Log.e(TAG, "Error fetching citation count by arXiv ID: $arxivId", e)
            0
        }
    }
} 