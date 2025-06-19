package com.lakshay.arxplorer.data.repository

import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    suspend fun addBookmark(paperId: String)
    suspend fun removeBookmark(paperId: String)
    suspend fun isBookmarked(paperId: String): Boolean
    suspend fun loadBookmarkedPaperIds()
    fun getBookmarkedPaperIds(): Flow<Set<String>>
} 