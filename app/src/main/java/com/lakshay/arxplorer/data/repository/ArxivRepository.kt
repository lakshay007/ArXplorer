package com.lakshay.arxplorer.data.repository

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.data.network.ArxivApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

private const val TAG = "ArxivRepository"

class ArxivRepository {
    private val api = ArxivApi()
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

            // Fetch papers for each preference in parallel
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
                                    sortBy = "submittedDate"
                                )
                                Log.d(TAG, "Result for $categoryCode: ${result.getOrNull()?.size ?: 0} papers")
                                result.getOrNull() ?: emptyList()
                            }
                        }
                    }

                val papers: List<List<ArxivPaper>> = deferredPapers.awaitAll()
                val flattenedPapers: List<ArxivPaper> = papers.flatten()
                    .sortedByDescending { paper: ArxivPaper -> paper.publishedDate }
                    .distinctBy { paper: ArxivPaper -> paper.id }

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
} 