package com.kidzone.utils

import androidx.annotation.StringRes

/**
 * Bazowy wyjątek dla błędów w repozytoriach, wspierający lokalizację.
 */
sealed class RepositoryException(@StringRes val messageRes: Int) : Exception() {

    class AlreadyReported(@StringRes resId: Int) : RepositoryException(resId)

    class Timeout(@StringRes resId: Int) : RepositoryException(resId)

    class OfflineSyncDisabled(@StringRes resId: Int) : RepositoryException(resId)
    
    class Generic(@StringRes resId: Int) : RepositoryException(resId)
}
