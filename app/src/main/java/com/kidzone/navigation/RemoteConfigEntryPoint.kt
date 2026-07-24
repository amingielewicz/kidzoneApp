package com.kidzone.navigation

import com.kidzone.data.remote.RemoteConfigService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 🎯 Odpowiedzialności:
 * - Punkt wejścia (EntryPoint) Hilt umożliwiający dostęp do [RemoteConfigService] poza ViewModelami.
 * - Umożliwia sprawdzenie statusu przerwy technicznej bezpośrednio w grafie nawigacji.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteConfigEntryPoint {
    fun remoteConfigService(): RemoteConfigService
}
