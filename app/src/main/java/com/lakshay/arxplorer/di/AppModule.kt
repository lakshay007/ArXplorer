package com.lakshay.arxplorer.di

import com.google.firebase.firestore.FirebaseFirestore
import com.lakshay.arxplorer.data.repository.ArxivRepository
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
    fun provideArxivRepository(): ArxivRepository {
        return ArxivRepository()
    }
} 