package com.kidzone.di

import com.kidzone.data.service.AndroidBadgePreferences
import com.kidzone.data.service.AndroidImageCompressor
import com.kidzone.data.service.AndroidLocationProvider
import com.kidzone.data.remote.PerformanceConfigProvider
import com.kidzone.data.remote.RemoteConfigService
import com.kidzone.domain.service.BadgePreferences
import com.kidzone.domain.service.ImageCompressorPort
import com.kidzone.domain.service.LocationProvider
import com.kidzone.i18n.AndroidLanguagePreferences
import com.kidzone.i18n.LanguagePreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding domain service interfaces to their Android implementations.
 *
 * These abstractions decouple ViewModels from Android Context, enabling
 * straightforward unit testing with MockK/fakes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindBadgePreferences(impl: AndroidBadgePreferences): BadgePreferences

    @Binds
    @Singleton
    abstract fun bindLanguagePreferences(impl: AndroidLanguagePreferences): LanguagePreferences

    @Binds
    @Singleton
    abstract fun bindImageCompressor(impl: AndroidImageCompressor): ImageCompressorPort

    @Binds
    @Singleton
    abstract fun bindPerformanceConfigProvider(impl: RemoteConfigService): PerformanceConfigProvider
}
