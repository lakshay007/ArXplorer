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

    // Add a companion object to store the remaining papers
    companion object {
        private var remainingNewPapers = listOf<ArxivPaper>()
        private var remainingPaperIds = listOf<String>()
        private var currentMode = "new"  // Can be "new" or "top"
        private const val PAPERS_PER_PAGE = 5
        private const val INITIAL_BATCH_SIZE = 50
    }

    suspend fun fetchPapersForUserPreferences(userId: String): Result<List<ArxivPaper>> {
        currentMode = "new"  // Set mode to new
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

            // Fetch papers for all preferences in a single query
            coroutineScope {
                // Combine all category codes into a single query with plus signs
                val categoryQuery = preferences
                    .mapNotNull { preference ->
                        val categoryCode = categoryMap[preference]
                        if (categoryCode == null) {
                            Log.w(TAG, "Unknown category preference: $preference")
                            null
                        } else {
                            categoryCode
                        }
                    }
                    .joinToString("+")

                Log.d(TAG, "Fetching papers for combined categories: $categoryQuery")
                
                // First try RSS feed
                val rssUrl = "https://rss.arxiv.org/atom/$categoryQuery"
                Log.d(TAG, "Trying RSS feed URL: $rssUrl")
                
                val rssResult = api.searchPapers(
                    query = rssUrl,
                    maxResults = INITIAL_BATCH_SIZE,
                    sortBy = "submittedDate",
                    sortOrder = "descending",
                    isRssQuery = true
                )

                val papers = when {
                    rssResult.isSuccess && rssResult.getOrNull()?.isNotEmpty() == true -> {
                        Log.d(TAG, "Successfully fetched ${rssResult.getOrNull()?.size} papers from RSS feed")
                        rssResult.getOrNull() ?: emptyList()
                    }
                    else -> {
                        // If RSS feed is empty or failed, fall back to regular API
                        val errorMessage = if (rssResult.isFailure) {
                            rssResult.exceptionOrNull()?.message ?: "Unknown error"
                        } else {
                            "RSS feed returned empty result"
                        }
                        Log.d(TAG, "RSS feed failed: $errorMessage, falling back to regular API")
                        
                        // Use the first category for the query
                        val searchQuery = "cat:${categoryQuery.split("+").first()}"
                        Log.d(TAG, "Falling back to arXiv API with query: $searchQuery")
                        
                        api.searchPapers(
                            query = searchQuery,
                            maxResults = INITIAL_BATCH_SIZE,
                            sortBy = "submittedDate",
                            sortOrder = "descending"
                        ).also { result ->
                            if (result.isSuccess) {
                                Log.d(TAG, "Successfully fetched ${result.getOrNull()?.size} papers from arXiv API")
                            } else {
                                Log.e(TAG, "ArXiv API fallback also failed: ${result.exceptionOrNull()?.message}")
                            }
                        }.getOrNull() ?: emptyList()
                    }
                }

                Log.d(TAG, "Result: ${papers.size} papers")

                // Store remaining papers for later
                if (papers.size > PAPERS_PER_PAGE) {
                    remainingNewPapers = papers.drop(PAPERS_PER_PAGE)
                    Log.d(TAG, "Stored ${remainingNewPapers.size} papers for later loading")
                } else {
                    remainingNewPapers = emptyList()
                }

                // Return first page
                val firstPage = papers.take(PAPERS_PER_PAGE)
                Log.d(TAG, "Returning first ${firstPage.size} papers")
                Result.success(firstPage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching papers", e)
            Result.failure(e)
        }
    }

    suspend fun fetchPaperDetails(paperId: String): Result<ArxivPaper> {
        return try {
            api.searchPapers(
                query = paperId,
                maxResults = 1,
                sortBy = "submittedDate",
                sortOrder = "descending",
                isIdQuery = true
            ).map { papers ->
                papers.firstOrNull() ?: throw Exception("Paper not found")
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        currentMode = "top"  // Set mode to top
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

            // Get the field of study for the first preference
            val categoryCode = categoryMap[preferences.first()]
            if (categoryCode == null) {
                Log.w(TAG, "Unknown category preference: ${preferences.first()}")
                return@coroutineScope Result.success(emptyList())
            }

            // Get all papers from Semantic Scholar
            val allPaperIds = semanticScholarApi.getTopPapersByField(
                field = categoryCode,
                fromDate = fromDate,
                untilDate = untilDate,
                limit = 100
            )

            // Store remaining IDs for pagination
            if (allPaperIds.size > PAPERS_PER_PAGE) {
                remainingPaperIds = allPaperIds.drop(PAPERS_PER_PAGE)
                Log.d(TAG, "Stored ${remainingPaperIds.size} paper IDs for later loading")
            } else {
                remainingPaperIds = emptyList()
            }

            // Get papers for first batch
            val paperIds = allPaperIds.take(PAPERS_PER_PAGE)
            Log.d(TAG, "Fetching papers for IDs: $paperIds")
            
            val result = api.searchPapers(
                query = paperIds.joinToString(","),
                maxResults = paperIds.size,
                isIdQuery = true
            )
            
            val papers = result.getOrNull() ?: emptyList()
            
            // Create a map of arXiv ID to paper for ordering
            val paperMap = papers.associateBy { paper -> 
                paper.id.split("v").first() // Remove version number if present
            }

            // Reorder papers according to Semantic Scholar order
            val orderedPapers = paperIds.mapNotNull { arxivId ->
                paperMap[arxivId].also { paper ->
                    if (paper == null) {
                        Log.w(TAG, "Could not find paper for arXiv ID: $arxivId")
                    }
                }
            }

            Log.d(TAG, "Returning ${orderedPapers.size} ordered papers")
            Result.success(orderedPapers)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top papers", e)
            Result.failure(e)
        }
    }

    // Add a function to load more papers
    suspend fun loadMorePapers(userId: String): Result<List<ArxivPaper>> = coroutineScope {
        try {
            when (currentMode) {
                "top" -> {
                    if (remainingPaperIds.isEmpty()) {
                        return@coroutineScope Result.success(emptyList())
                    }

                    val nextBatch = remainingPaperIds.take(20)
                    remainingPaperIds = remainingPaperIds.drop(20)

                    val query = nextBatch.joinToString(" OR ") { "id:$it" }
                    Log.d(TAG, "Fetching next batch of top arXiv papers with query: $query")
                    
                    val papers = api.searchPapers(
                        query = query,
                        maxResults = nextBatch.size
                    ).getOrNull() ?: emptyList()

                    // Create a map of arXiv ID to paper
                    val paperMap = papers.associateBy { paper -> 
                        // Extract ID from various possible formats
                        when {
                            paper.id.contains("/abs/") -> paper.id.substringAfter("/abs/")
                            paper.id.contains("arXiv:") -> paper.id.substringAfter("arXiv:")
                            else -> paper.id
                        }.split("v").first() // Remove version number if present
                    }

                    // Reorder papers according to Semantic Scholar order
                    val orderedPapers = nextBatch.mapNotNull { arxivId ->
                        paperMap[arxivId].also { paper ->
                            if (paper == null) {
                                Log.w(TAG, "Could not find paper for arXiv ID: $arxivId")
                            }
                        }
                    }

                    Log.d(TAG, "Returning ${orderedPapers.size} ordered papers")
                    Result.success(orderedPapers)
                }
                "new" -> {
                    if (remainingNewPapers.isEmpty()) {
                        return@coroutineScope Result.success(emptyList())
                    }
                    // Load next batch of new papers from our stored list
                    val nextBatch = remainingNewPapers.take(PAPERS_PER_PAGE)
                    remainingNewPapers = remainingNewPapers.drop(PAPERS_PER_PAGE)
                    Log.d(TAG, "Loading next ${nextBatch.size} new papers, ${remainingNewPapers.size} remaining")
                    Result.success(nextBatch)
                }
                else -> Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more papers", e)
            Result.failure(e)
        }
    }

    suspend fun searchArxiv(
        query: String,
        sortBy: String = "relevance",
        sortOrder: String = "descending",
        isTitleSearch: Boolean = false
    ): Result<List<ArxivPaper>> {
        return api.searchPapers(
            query = query,
            maxResults = INITIAL_BATCH_SIZE,
            sortBy = sortBy,
            sortOrder = sortOrder,
            isTitleSearch = isTitleSearch
        )
    }

    suspend fun getPaperById(paperId: String): ArxivPaper {
        return fetchPaperDetails(paperId).getOrThrow()
    }

    fun getNextBatchOfPapers(): List<ArxivPaper> {
        val nextBatch = remainingNewPapers.take(PAPERS_PER_PAGE)
        remainingNewPapers = remainingNewPapers.drop(PAPERS_PER_PAGE)
        return nextBatch
    }

    fun hasMoreTopPapers(): Boolean {
        return remainingPaperIds.isNotEmpty()
    }

    suspend fun getNextBatchOfTopPapers(): Result<List<ArxivPaper>> = coroutineScope {
        try {
            if (remainingPaperIds.isEmpty()) {
                return@coroutineScope Result.success(emptyList())
            }

            val nextBatch = remainingPaperIds.take(PAPERS_PER_PAGE)
            remainingPaperIds = remainingPaperIds.drop(PAPERS_PER_PAGE)

            // Fetch all papers in a single API call
            val result = api.searchPapers(
                query = nextBatch.joinToString(","),
                maxResults = nextBatch.size,
                isIdQuery = true
            )
            
            val papers = result.getOrNull() ?: emptyList()
            
            // Create a map of arXiv ID to paper for ordering
            val paperMap = papers.associateBy { paper -> 
                paper.id.split("v").first() // Remove version number if present
            }

            // Reorder papers according to Semantic Scholar order
            val orderedPapers = nextBatch.mapNotNull { arxivId ->
                paperMap[arxivId].also { paper ->
                    if (paper == null) {
                        Log.w(TAG, "Could not find paper for arXiv ID: $arxivId")
                    }
                }
            }

            Result.success(orderedPapers)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading next batch of top papers", e)
            Result.failure(e)
        }
    }

    fun clearRemainingPapers() {
        remainingNewPapers = emptyList()
        remainingPaperIds = emptyList()
    }
}

enum class TimePeriod {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
} 