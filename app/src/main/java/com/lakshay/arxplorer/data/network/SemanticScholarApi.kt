package com.lakshay.arxplorer.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "SemanticScholarApi"
private const val BASE_URL = "https://api.semanticscholar.org/graph/v1"

class SemanticScholarApi {
    private val client = OkHttpClient()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val inputDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-ddXXX")

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
            // Convert arXiv category to appropriate Semantic Scholar field of study
            val fieldOfStudy = when {
                field.startsWith("cs.") -> "Computer Science"
                field.startsWith("math.") -> "Mathematics"
                field.startsWith("physics") || field.startsWith("astro") || 
                field.startsWith("quant") || field.startsWith("hep") -> "Physics"
                field.startsWith("cond-mat") -> "Materials Science"
                field.startsWith("q-bio") || field.startsWith("bio") -> "Biology"
                field.startsWith("q-fin") -> "Economics"
                field.startsWith("stat") -> "Mathematics"  // Statistics falls under Mathematics
                else -> "Computer Science"  // Default to Computer Science
            }

            // Build date filter
            val dateFilter = if (fromDate != null && untilDate != null) {
                try {
                    // Remove the timezone offset from the date string
                    val cleanFromDate = fromDate.substringBefore("+")
                    val cleanUntilDate = untilDate.substringBefore("+")
                    
                    // Parse as LocalDate since we don't need timezone information
                    val formattedFromDate = LocalDate.parse(cleanFromDate).format(dateFormatter)
                    val formattedUntilDate = LocalDate.parse(cleanUntilDate).format(dateFormatter)
                    
                    "&publicationDateOrYear=$formattedFromDate:$formattedUntilDate"
                } catch (e: Exception) {
                    Log.e(TAG, "Error formatting dates: $e")
                    ""
                }
            } else ""

            // Build URL with all necessary parameters
            val url = "$BASE_URL/paper/search/bulk?" +
                     "fieldsOfStudy=$fieldOfStudy" +
                     "&fields=title,citationCount,externalIds,publicationDate,openAccessPdf,isOpenAccess" +
                     "&openAccessPdf" +
                     dateFilter +
                     "&sort=citationCount:desc" +
                     "&limit=$limit"

            Log.d(TAG, "Making Semantic Scholar bulk request for field: $fieldOfStudy")
            Log.d(TAG, "URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ArXplorer/1.0")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Error searching papers: ${response.code}")
                Log.e(TAG, "Response body: $responseBody")
                return@withContext emptyList()
            }

            val jsonResponse = JSONObject(responseBody ?: return@withContext emptyList())
            Log.d(TAG, "Response: $responseBody")
            val papers = jsonResponse.getJSONArray("data")
            val total = jsonResponse.optInt("total", 0)
            Log.d(TAG, "Found $total total papers, fetched ${papers.length()} papers")

            val arxivPapers = mutableListOf<String>()
            
            for (i in 0 until papers.length()) {
                val paper = papers.getJSONObject(i)
                
                // First try to get arXiv ID from externalIds
                val externalIds = paper.optJSONObject("externalIds")
                if (externalIds?.has("ArXiv") == true) {
                    arxivPapers.add(externalIds.getString("ArXiv"))
                    continue
                }
                
                // If no ArXiv ID in externalIds, try to extract from PDF URL
                val openAccessPdf = paper.optJSONObject("openAccessPdf")
                val pdfUrl = openAccessPdf?.optString("url", "")
                if (pdfUrl?.contains("arxiv.org") == true) {
                    // Extract arXiv ID from URL
                    // URL format: http://arxiv.org/pdf/2305.06488
                    val arxivId = pdfUrl.substringAfterLast("/").removeSuffix(".pdf")
                    arxivPapers.add(arxivId)
                }
            }
            
            Log.d(TAG, "Found ${arxivPapers.size} arXiv papers with IDs: $arxivPapers")
            arxivPapers
        } catch (e: Exception) {
            Log.e(TAG, "Error searching top papers: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    private fun getCitationCountByDoi(doi: String): Int {
        val request = Request.Builder()
            .url("$BASE_URL/paper/DOI:$doi?fields=citationCount")
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