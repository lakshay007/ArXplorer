package com.lakshay.arxplorer.data.repository

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.network.ArxivApi
import com.lakshay.arxplorer.data.network.CrossrefApi
import com.lakshay.arxplorer.data.network.SemanticScholarApi
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

private const val TAG = "ArxivRepository"

class ArxivRepository {
    private val api = ArxivApi()
    private val crossrefApi = CrossrefApi()
    private val semanticScholarApi = SemanticScholarApi()
    private val firestore = Firebase.firestore

    // Map user-friendly names to arXiv category codes
    private val categoryMap = mapOf(
        // Computer Science
        "Artificial Intelligence" to "cs.AI",
        "Computation and Language" to "cs.CL",
        "Computer Vision and Pattern Recognition" to "cs.CV",
        "Cryptography and Security" to "cs.CR",
        "Databases" to "cs.DB",
        "Human-Computer Interaction" to "cs.HC",
        "Machine Learning" to "cs.LG",
        "Programming Languages" to "cs.PL",
        "Robotics" to "cs.RO",
        "Software Engineering" to "cs.SE",

        // Physics
        "Astrophysics" to "astro-ph",
        "Quantum Physics" to "quant-ph",
        "High Energy Physics" to "hep-th",
        "Nuclear Physics" to "nucl-th",
        "Condensed Matter Physics" to "cond-mat",
        "Mathematical Physics" to "math-ph",
        "Applied Physics" to "physics.app-ph",
        "Computational Physics" to "physics.comp-ph",

        // Mathematics
        "Algebra" to "math.AG",
        "Geometry" to "math.GE",
        "Number Theory" to "math.NT",
        "Analysis" to "math.AN",
        "Probability" to "math.PR",
        "Statistics" to "math.ST",
        "Logic" to "math.LO",
        "Combinatorics" to "math.CO"
    )

    suspend fun fetchPapersForUserPreferences(userId: String): Result<List<ArxivPaper>> {
        return try {
            Log.d(TAG, "Fetching preferences for user: $userId")
            // Get user preferences from Firestore
            val preferencesDoc = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            if (!preferencesDoc.exists()) {
                Log.d(TAG, "No preferences document found for user: $userId")
                return Result.success(emptyList())
            }

            @Suppress("UNCHECKED_CAST")
            val preferences = preferencesDoc.get("preferences") as? List<String>
            if (preferences == null) {
                Log.d(TAG, "Invalid preferences format for user: $userId")
                return Result.success(emptyList())
            }

            if (preferences.isEmpty()) {
                Log.d(TAG, "User has no preferences set")
                return Result.success(emptyList())
            }

            Log.d(TAG, "Found preferences: $preferences")

            // Fetch papers for each preference in parallel with a small delay between requests
            coroutineScope {
                val deferredPapers: List<Deferred<List<ArxivPaper>>> = preferences
                    .mapNotNull { preference ->
                        val categoryCode = categoryMap[preference]
                        if (categoryCode == null) {
                            Log.w(TAG, "Unknown category preference: $preference")
                            null
                        } else {
                            async {
                                Log.d(TAG, "Fetching papers for category: $categoryCode (from: $preference)")
                                val result = api.searchPapers(
                                    query = "cat:$categoryCode",
                                    maxResults = 5,
                                    sortBy = "lastUpdatedDate",  // Use lastUpdatedDate instead of submittedDate
                                    sortOrder = "descending"
                                )
                                Log.d(TAG, "Result for $categoryCode: ${result.getOrNull()?.size ?: 0} papers")
                                result.getOrNull() ?: emptyList()
                            }
                        }
                    }

                val papers: List<List<ArxivPaper>> = deferredPapers.awaitAll()
                val flattenedPapers: List<ArxivPaper> = papers.flatten()
                    .sortedByDescending { it.updatedDate }  // Sort by updated date
                    .distinctBy { it.id }

                Log.d(TAG, "Total papers fetched: ${flattenedPapers.size}")
                Result.success(flattenedPapers)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching papers", e)
            Result.failure(e)
        }
    }

    suspend fun fetchPaperDetails(paperId: String): Result<ArxivPaper> {
        return api.searchPapers(
            query = "id:$paperId",
            maxResults = 1
        ).map { papers ->
            papers.firstOrNull() ?: throw Exception("Paper not found")
        }
    }

    suspend fun fetchLatestPapers(category: String, maxResults: Int = 10): Result<List<ArxivPaper>> {
        val categoryCode = categoryMap[category] ?: category
        return api.searchPapers(
            query = "cat:$categoryCode",
            maxResults = maxResults,
            sortBy = "submittedDate"
        )
    }

    suspend fun hasUserPreferences(userId: String): Boolean {
        return try {
            val preferencesDoc = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            if (!preferencesDoc.exists()) {
                Log.d(TAG, "No preferences document found for user: $userId")
                return false
            }

            @Suppress("UNCHECKED_CAST")
            val preferences = preferencesDoc.get("preferences") as? List<String>
            if (preferences == null || preferences.isEmpty()) {
                Log.d(TAG, "No preferences set for user: $userId")
                return false
            }

            Log.d(TAG, "Found preferences for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user preferences", e)
            false
        }
    }

    suspend fun fetchTopPapersForUserPreferences(
        userId: String,
        timePeriod: TimePeriod
    ): Result<List<ArxivPaper>> = coroutineScope {
        try {
            // Get user preferences
            val preferencesDoc = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val preferences = preferencesDoc.get("preferences") as? List<String>
            if (preferences.isNullOrEmpty()) {
                return@coroutineScope Result.success(emptyList())
            }

            // Get date range based on time period
            val now = ZonedDateTime.now()
            val (fromDate, untilDate) = when (timePeriod) {
                TimePeriod.THIS_WEEK -> {
                    val weekAgo = now.minusDays(7)
                    Pair(weekAgo.format(DateTimeFormatter.ISO_DATE),
                         now.format(DateTimeFormatter.ISO_DATE))
                }
                TimePeriod.THIS_MONTH -> {
                    val monthAgo = now.minusMonths(1)
                    Pair(monthAgo.format(DateTimeFormatter.ISO_DATE),
                         now.format(DateTimeFormatter.ISO_DATE))
                }
                TimePeriod.THIS_YEAR -> {
                    val yearAgo = now.minusYears(1)
                    Pair(yearAgo.format(DateTimeFormatter.ISO_DATE),
                         now.format(DateTimeFormatter.ISO_DATE))
                }
                TimePeriod.ALL_TIME -> Pair(null, now.format(DateTimeFormatter.ISO_DATE))
            }

            Log.d(TAG, "Date range: from=$fromDate, until=$untilDate")

            // Fetch papers for each preference
            val deferredPapers: List<Deferred<List<ArxivPaper>>> = preferences.mapNotNull { preference ->
                val categoryCode = categoryMap[preference]
                if (categoryCode == null) {
                    Log.w(TAG, "Unknown category preference: $preference")
                    null
                } else {
                    async<List<ArxivPaper>> {
                        // First get top cited papers from Semantic Scholar
                        val topPaperIds = semanticScholarApi.getTopPapersByField(
                            field = categoryCode,
                            fromDate = fromDate,
                            untilDate = untilDate,
                            limit = 20
                        )

                        if (topPaperIds.isEmpty()) {
                            emptyList()
                        } else {
                            // Then fetch those papers from arXiv
                            val query = topPaperIds.joinToString(" OR ") { "id:$it" }
                            Log.d(TAG, "Fetching arXiv papers with query: $query")
                            api.searchPapers(
                                query = query,
                                maxResults = topPaperIds.size
                            ).getOrNull() ?: emptyList()
                        }
                    }
                }
            }

            val allPapers: List<List<ArxivPaper>> = deferredPapers.awaitAll()
            val papers: List<ArxivPaper> = allPapers.flatten()
                .distinctBy { paper -> paper.id }

            Result.success(papers)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top papers", e)
            Result.failure(e)
        }
    }

    suspend fun searchArxiv(
        query: String,
        maxResults: Int = 20,
        sortBy: String = "all",  // Changed default to "all"
        sortOrder: String = "descending"  // Can be "ascending" or "descending"
    ): Result<List<ArxivPaper>> {
        return try {
            Log.d(TAG, "Searching arXiv for query: $query")
            // Construct search query based on sortBy parameter
            val searchQuery = when (sortBy) {
                "title" -> "ti:$query"
                "all" -> "all:$query"
                else -> "all:$query"  // Default to all fields if unknown sortBy value
            }
            
            api.searchPapers(
                query = searchQuery,
                maxResults = maxResults,
                sortBy = if (sortBy in listOf("all", "title")) "relevance" else sortBy,
                sortOrder = sortOrder
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error searching papers", e)
            Result.failure(e)
        }
    }
}

enum class TimePeriod {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
} 