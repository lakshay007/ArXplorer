package com.lakshay.arxplorer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BookmarkRepository {
    private val bookmarkedPaperIds = MutableStateFlow<Set<String>>(emptySet())
    
    override suspend fun addBookmark(paperId: String) {
        val userId = auth.currentUser?.uid ?: return
        val bookmarkRef = firestore.collection("users").document(userId)
            .collection("bookmarks").document(paperId)
            
        val bookmarkData = hashMapOf(
            "paperId" to paperId,
            "timestamp" to System.currentTimeMillis()
        )
        
        bookmarkRef.set(bookmarkData).await()
        
        // Update local cache
        val currentBookmarks = bookmarkedPaperIds.value.toMutableSet()
        currentBookmarks.add(paperId)
        bookmarkedPaperIds.value = currentBookmarks
    }
    
    override suspend fun removeBookmark(paperId: String) {
        val userId = auth.currentUser?.uid ?: return
        val bookmarkRef = firestore.collection("users").document(userId)
            .collection("bookmarks").document(paperId)
            
        bookmarkRef.delete().await()
        
        // Update local cache
        val currentBookmarks = bookmarkedPaperIds.value.toMutableSet()
        currentBookmarks.remove(paperId)
        bookmarkedPaperIds.value = currentBookmarks
    }
    
    override suspend fun isBookmarked(paperId: String): Boolean {
        if (bookmarkedPaperIds.value.contains(paperId)) {
            return true
        }
        
        val userId = auth.currentUser?.uid ?: return false
        val bookmarkRef = firestore.collection("users").document(userId)
            .collection("bookmarks").document(paperId)
            
        val document = bookmarkRef.get().await()
        return document.exists()
    }
    
    override suspend fun loadBookmarkedPaperIds() {
        val userId = auth.currentUser?.uid ?: return
        val bookmarksRef = firestore.collection("users").document(userId)
            .collection("bookmarks")
            
        val documents = bookmarksRef.get().await()
        val paperIds = documents.documents.mapNotNull { it.id }.toSet()
        bookmarkedPaperIds.value = paperIds
    }
    
    override fun getBookmarkedPaperIds(): Flow<Set<String>> = bookmarkedPaperIds
} 