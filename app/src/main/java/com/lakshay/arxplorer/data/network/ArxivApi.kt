package com.lakshay.arxplorer.data.network

import android.util.Log
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "ArxivApi"

class ArxivApi {
    private val baseUrl = "https://export.arxiv.org/api/query"

    private fun formatPaperId(id: String): String {
        // Handle old-style IDs (pre-2007)
        return if (id.matches(Regex("^\\d{7}$"))) {
            // Convert YYMMNNN to YYMM.NNNNN format
            // For old papers: YYMMNNN where YY=year, MM=month, NNN=number
            val year = "19" + id.substring(0, 2)
            val month = id.substring(2, 4)
            val number = id.substring(4).padStart(5, '0') // Ensure 5 digits for the number part
            "$year.$number"
        } else {
            id
        }
    }

    suspend fun searchPapers(
        query: String,
        start: Int = 0,
        maxResults: Int = 10,
        sortBy: String = "submittedDate",
        sortOrder: String = "descending",
        isIdQuery: Boolean = false,
        isRssQuery: Boolean = false,
        isTitleSearch: Boolean = false
    ): Result<List<ArxivPaper>> = withContext(Dispatchers.IO) {
        try {
            delay(100)
            
            val searchUrl = when {
                isRssQuery -> query
                isIdQuery -> {
                    val formattedIds = query.split(",").map { formatPaperId(it) }.joinToString(",")
                    "$baseUrl?id_list=$formattedIds"
                }
                else -> buildSearchUrl(query, start, maxResults, sortBy, sortOrder, isTitleSearch)
            }
            
            Log.d(TAG, "Making API request to: $searchUrl")
            
            val papers = fetchAndParsePapers(searchUrl)
            Log.d(TAG, "Successfully fetched ${papers.size} papers for query: $query")
            Result.success(papers)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching papers for query: $query", e)
            Result.failure(e)
        }
    }

    private fun buildSearchUrl(
        query: String,
        start: Int,
        maxResults: Int,
        sortBy: String,
        sortOrder: String,
        isTitleSearch: Boolean
    ): String {
        val formattedQuery = if (isTitleSearch) {
            // For title search, wrap the entire query in quotes
            "ti:\"${query.trim()}\""
        } else if (query.startsWith("cat:")) {
            // For category queries, use as is
            query
        } else {
            // For general search, prefix each term with all: and join with +AND+
            query.trim()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .map { java.net.URLEncoder.encode("all:$it", "UTF-8") }
                .joinToString("+AND+")
        }
            
        val baseQuery = "$baseUrl?search_query=$formattedQuery" +
                       "&start=$start" +
                       "&max_results=$maxResults"

        // Only add sorting parameters for non-title searches
        return if (isTitleSearch) {
            baseQuery
        } else {
            baseQuery +
            "&sortBy=submittedDate" +
            "&sortOrder=descending" +
            "&include_cross_list=true"
        }
    }

    private fun fetchAndParsePapers(urlString: String): List<ArxivPaper> {
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.setRequestProperty("User-Agent", "ArXplorer/1.0 (Android App)")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val inputStream = connection.getInputStream()
        val doc = builder.parse(inputStream)

        val entries = doc.getElementsByTagName("entry")
        Log.d(TAG, "Found ${entries.length} entries in response")
        
        if (entries.length == 0) {
            // Log the raw response for debugging
            inputStream.reset()
            val response = inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Raw API response: $response")
        }
        
        val papers = mutableListOf<ArxivPaper>()

        for (i in 0 until entries.length) {
            try {
                val entry = entries.item(i) as Element
                val paper = parsePaper(entry)
                papers.add(paper)
                Log.d(TAG, "Successfully parsed paper: ${paper.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing paper at index $i", e)
            }
        }

        return papers
    }

    private fun parsePaper(entry: Element): ArxivPaper {
        val rawId = entry.getElementsByTagName("id").item(0).textContent
        val id = when {
            rawId.contains("oai:arXiv.org:") -> rawId.substringAfter("oai:arXiv.org:")
            rawId.contains("/abs/") -> rawId.substringAfterLast("/")
            else -> rawId.split("/").last()
        }
        
        val title = entry.getElementsByTagName("title").item(0).textContent.trim()
        val rawSummary = entry.getElementsByTagName("summary").item(0).textContent.trim()
        
        // Clean up the summary by removing arXiv ID, announcement type, and labels
        val summary = rawSummary
            .replace(Regex("^\\s*arXiv:\\s*\\S+\\s*"), "") // Remove arXiv ID from start
            .replace(Regex("(?i)Announce Type:\\s*[^\\n]*"), "") // Remove announcement type without requiring newline
            .replace(Regex("(?i)\\s*Abstract:\\s*"), "") // Remove Abstract: label
            .replace(Regex("^\\s*\\([^)]*\\)\\s*"), "") // Remove parenthetical announcement type
            .replace(Regex("^\\s*New\\s*"), "") // Remove standalone "New" text
            .trim()
            
        val published = parseDateTime(entry.getElementsByTagName("published").item(0).textContent)
        val updated = parseDateTime(entry.getElementsByTagName("updated").item(0).textContent)
        
        val authors = entry.getElementsByTagName("author").let { authorNodes ->
            (0 until authorNodes.length).map { i ->
                authorNodes.item(i).childNodes.item(0).textContent
            }
        }

        val links = entry.getElementsByTagName("link")
        val pdfUrl = (0 until links.length)
            .map { links.item(it) as Element }
            .firstOrNull { 
                it.getAttribute("title") == "pdf" || 
                it.getAttribute("type") == "application/pdf" ||
                it.getAttribute("href").endsWith(".pdf")
            }
            ?.getAttribute("href")
            ?: "https://arxiv.org/pdf/$id.pdf" // Fallback URL construction using clean ID

        val categories = entry.getElementsByTagName("category").let { categoryNodes ->
            (0 until categoryNodes.length).map { i ->
                (categoryNodes.item(i) as Element).getAttribute("term")
            }
        }

        val primaryCategory = categories.firstOrNull() ?: id.split(".").first()
        
        // Optional fields
        val doi = entry.getElementsByTagName("arxiv:doi").let { 
            if (it.length > 0) it.item(0).textContent else null 
        }
        
        val comment = entry.getElementsByTagName("arxiv:comment").let { 
            if (it.length > 0) it.item(0).textContent else null 
        }
        
        val journalRef = entry.getElementsByTagName("arxiv:journal_ref").let { 
            if (it.length > 0) it.item(0).textContent else null 
        }

        Log.d(TAG, "Parsed paper - ID: $id, Title: $title, Category: $primaryCategory")

        return ArxivPaper(
            id = id,
            title = title,
            authors = authors,
            summary = summary,
            publishedDate = published,
            updatedDate = updated,
            pdfUrl = pdfUrl,
            categories = categories,
            doi = doi,
            primaryCategory = primaryCategory,
            abstract = summary,
            commentaries = comment,
            journalRef = journalRef
        )
    }

    private fun parseDateTime(dateStr: String): ZonedDateTime {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
    }
} 