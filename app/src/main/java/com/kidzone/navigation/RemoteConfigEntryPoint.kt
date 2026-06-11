package com.kidzone.navigation

import com.kidzone.data.remote.RemoteConfigService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing [RemoteConfigService] in Compose contexts
 * that don't have a ViewModel (e.g., NavGraph maintenance gate).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteConfigEntryPoint {
    fun remoteConfigService(): RemoteConfigService
}
