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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Dostarczanie instancji bazy danych Room ([KidZoneDatabase]) i jej obiektów DAO.
 * - Konfiguracja globalnego [ApplicationScope] dla operacji asynchronicznych.
 *
 * ✅ Gwarancje:
 * - Singletony dla wszystkich obiektów dostępu do danych.
 * - Bezpieczne czyszczenie bazy przy zmianach schematu (Destructive Migration).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
