package com.lakshay.arxplorer.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lakshay.arxplorer.data.repository.ArxivRepository
import com.lakshay.arxplorer.data.repository.BookmarkRepository
import com.lakshay.arxplorer.data.repository.BookmarkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideArxivRepository(): ArxivRepository {
        return ArxivRepository()
    }
    
    @Provides
    @Singleton
    fun provideBookmarkRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): BookmarkRepository {
        return BookmarkRepositoryImpl(firestore, auth)
    }
} 