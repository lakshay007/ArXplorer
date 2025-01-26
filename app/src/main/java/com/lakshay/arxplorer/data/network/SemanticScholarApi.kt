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
            // Convert arXiv category to appropriate Semantic Scholar query terms
            val rawQuery = when (field) {
                // Computer Science fields
                "cs.AI" -> "\"artificial intelligence\""
                "cs.CV" -> "\"computer vision\""
                "cs.CL" -> "\"natural language processing\""
                "cs.RO" -> "\"robotics\""
                "cs.LG" -> "\"machine learning\""
                "cs.CR" -> "\"cryptography\""
                "cs.DB" -> "\"database systems\""
                "cs.HC" -> "\"human computer interaction\""
                "cs.PL" -> "\"programming languages\""
                "cs.SE" -> "\"software engineering\""

                // Physics fields
                "astro-ph" -> "\"astrophysics\""
                "quant-ph" -> "\"quantum physics\""
                "hep-th" -> "\"high energy physics\""
                "nucl-th" -> "\"nuclear physics\""
                "cond-mat" -> "\"condensed matter physics\""
                "math-ph" -> "\"mathematical physics\""
                "physics.app-ph" -> "\"applied physics\""
                "physics.comp-ph" -> "\"computational physics\""

                // Mathematics fields
                "math.AG" -> "\"algebraic geometry\""
                "math.GE" -> "\"geometry\""
                "math.NT" -> "\"number theory\""
                "math.AN" -> "\"mathematical analysis\""
                "math.PR" -> "\"probability theory\""
                "math.ST" -> "\"statistics\""
                "math.LO" -> "\"mathematical logic\""
                "math.CO" -> "\"combinatorics\""

                // Default case: use the field name without the prefix
                else -> "\"${field.substringAfter(".").replace(".", " ")}\""
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

            // Build URL with all necessary parameters - using the exact working format
            val url = "$BASE_URL/paper/search/bulk?query=$rawQuery" +
                     "&fields=title,citationCount,externalIds,publicationDate,openAccessPdf,isOpenAccess" +
                     "&openAccessPdf" +
                     dateFilter +
                     "&sort=citationCount:desc" +
                     "&limit=$limit"

            Log.d(TAG, "Making Semantic Scholar bulk request with query: $rawQuery")
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

            val arxivIds = mutableListOf<String>()
            for (i in 0 until papers.length()) {
                val paper = papers.getJSONObject(i)
                val citationCount = paper.optInt("citationCount", 0)
                val externalIds = paper.optJSONObject("externalIds")
                val publicationDate = paper.optString("publicationDate", "")
                val title = paper.optString("title", "")
                val isOpenAccess = paper.optBoolean("isOpenAccess", false)
                val openAccessPdf = paper.optJSONObject("openAccessPdf")
                
                if (externalIds != null && externalIds.has("ArXiv")) {
                    val arxivId = externalIds.getString("ArXiv")
                    Log.d(TAG, "Found paper: '$title' with arXiv ID: $arxivId, citations: $citationCount, date: $publicationDate, isOpenAccess: $isOpenAccess")
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