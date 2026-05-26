package com.playword.di

import com.playword.data.repository.FirebaseAuthRepository
import com.playword.data.repository.FirestorePlaceRepository
import com.playword.data.repository.FirestoreReviewRepository
import com.playword.domain.repository.AuthRepository
import com.playword.domain.repository.PlaceRepository
import com.playword.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wiąże implementacje Firebase z interfejsami warstwy domain.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(impl: FirestorePlaceRepository): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: FirestoreReviewRepository): ReviewRepository
}
