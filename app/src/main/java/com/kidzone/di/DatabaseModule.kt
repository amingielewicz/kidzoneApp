package com.kidzone.di

import android.content.Context
import androidx.room.Room
import com.kidzone.data.local.KidZoneDatabase
import com.kidzone.data.local.PlaceDao
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.sync.PendingOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module dostarczający Room database i DAO.
 *
 * `fallbackToDestructiveMigration()` – cache można bezpiecznie odbudować
 * z Firestore, więc przy zmianie schematu po prostu czyścimy bazę zamiast
 * pisać migracje (upraszcza development w fazie MVP).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KidZoneDatabase =
        Room.databaseBuilder(
            context,
            KidZoneDatabase::class.java,
            "kidzone_cache.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun providePlaceDao(database: KidZoneDatabase): PlaceDao =
        database.placeDao()

    @Provides
    @Singleton
    fun provideReviewDao(database: KidZoneDatabase): ReviewDao =
        database.reviewDao()

    @Provides
    @Singleton
    fun providePendingOperationDao(database: KidZoneDatabase): PendingOperationDao =
        database.pendingOperationDao()
}
