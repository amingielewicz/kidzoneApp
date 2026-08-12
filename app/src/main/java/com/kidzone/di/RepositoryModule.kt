package com.kidzone.di

import com.kidzone.data.repository.FirebaseAuthRepository
import com.kidzone.data.repository.FirestorePlaceRepository
import com.kidzone.data.repository.FirestoreReviewRepository
import com.kidzone.data.repository.RetrofitIpLocationRepository
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.IpLocationRepository
import com.kidzone.domain.repository.PlaceRepository
import com.kidzone.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Wiązanie implementacji warstwy danych (Firebase/Room) z interfejsami warstwy domeny.
 *
 * ✅ Gwarancje:
 * - Singletony dla wszystkich repozytoriów, zapewniające spójność stanu danych w całej aplikacji.
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

    @Binds
    @Singleton
    abstract fun bindIpLocationRepository(impl: RetrofitIpLocationRepository): IpLocationRepository
}
