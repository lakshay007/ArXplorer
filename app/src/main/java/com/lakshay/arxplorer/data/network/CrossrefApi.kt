package com.lakshay.arxplorer.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

private const val TAG = "CrossrefApi"
private const val BASE_URL = "https://api.crossref.org/works"

class CrossrefApi {
    private val client = OkHttpClient()

    suspend fun getTopPapersByField(
        field: String,
        fromDate: String? = null,
        untilDate: String? = null,
        limit: Int = 20
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            // Build date filter
            val filters = mutableListOf("has-abstract:true", "has-references:true")
            if (fromDate != null && untilDate != null) {
                filters.add("from-pub-date:$fromDate")
                filters.add("until-pub-date:$untilDate")
            }

            // Convert field to appropriate subject
            val subject = when {
                field.startsWith("cs.") -> "computer science"
                field.startsWith("math.") -> "mathematics"
                field.startsWith("physics") || field.startsWith("astro") || 
                field.startsWith("quant") || field.startsWith("hep") -> "physics"
                else -> field.replace(".", " ").lowercase()
            }

            // Build URL with filters - add arXiv to query to find papers that mention it
            val url = "$BASE_URL?query=$subject+arXiv" +
                     "&filter=${filters.joinToString(",")}" +
                     "&select=DOI,title,reference,URL,is-referenced-by-count,abstract" +
                     "&sort=is-referenced-by-count" +
                     "&order=desc" +
                     "&rows=$limit" +
                     "&mailto=lakshay.arora.developer@gmail.com"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ArXplorer/1.0 (mailto:lakshay.arora.developer@gmail.com)")
                .build()

            Log.d(TAG, "Making Crossref request: $url")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Error searching papers: ${response.code}")
                Log.e(TAG, "Response body: $responseBody")
                return@withContext emptyList()
            }

            val jsonResponse = JSONObject(responseBody ?: return@withContext emptyList())
            val items = jsonResponse.getJSONObject("message").getJSONArray("items")
            Log.d(TAG, "Found ${items.length()} papers in total")

            val arxivIds = mutableSetOf<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val citationCount = item.optInt("is-referenced-by-count", 0)
                
                // Check URLs first
                val urls = item.optJSONArray("URL")
                if (urls != null) {
                    for (j in 0 until urls.length()) {
                        val urlStr = urls.getString(j)
                        if (urlStr.contains("arxiv.org")) {
                            val arxivId = extractArxivId(urlStr)
                            if (arxivId != null) {
                                Log.d(TAG, "Found arXiv ID in URL: $arxivId with $citationCount citations")
                                arxivIds.add(arxivId)
                            }
                        }
                    }
                }

                // Check references for arXiv IDs
                val references = item.optJSONArray("reference")
                if (references != null) {
                    for (j in 0 until references.length()) {
                        val ref = references.getJSONObject(j)
                        val unstructured = ref.optString("unstructured", "")
                        if (unstructured.contains("arXiv:")) {
                            val arxivId = extractArxivId(unstructured)
                            if (arxivId != null) {
                                Log.d(TAG, "Found arXiv ID in reference: $arxivId")
                                arxivIds.add(arxivId)
                            }
                        }
                    }
                }

                // Also check abstract for arXiv mentions
                val abstract = item.optString("abstract", "")
                if (abstract.contains("arXiv:")) {
                    val arxivId = extractArxivId(abstract)
                    if (arxivId != null) {
                        Log.d(TAG, "Found arXiv ID in abstract: $arxivId with $citationCount citations")
                        arxivIds.add(arxivId)
                    }
                }
            }

            Log.d(TAG, "Found ${arxivIds.size} unique arXiv papers from Crossref")
            arxivIds.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching top papers: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    private fun extractArxivId(text: String): String? {
        // Match patterns like:
        // arXiv:2308.08434
        // arxiv.org/abs/2308.08434
        // arxiv.org/pdf/2308.08434
        val regex = Regex("""(?:arXiv:|arxiv\.org/(?:abs|pdf)/)(\d{4}\.\d{4,5})""")
        return regex.find(text)?.groupValues?.get(1)
    }
} 