package com.kidzone

import leakcanary.LeakCanary
import shark.IgnoredReferenceMatcher
import shark.ReferenceMatcher
import shark.ReferencePattern
import timber.log.Timber

object DebugTools {

    @JvmStatic
    fun install() {
        configureLeakCanary()
    }

    private fun configureLeakCanary() {
        LeakCanary.config = LeakCanary.config.copy(
            retainedVisibleThreshold = RETAINED_VISIBLE_THRESHOLD,
            referenceMatchers = LeakCanary.config.referenceMatchers + xiaomiResourcesImplMatcher()
        )
        Timber.d("LeakCanary debug filters installed")
    }

    private fun xiaomiResourcesImplMatcher(): ReferenceMatcher =
        IgnoredReferenceMatcher(
            ReferencePattern.InstanceFieldPattern(
                className = "android.content.res.ResourcesImpl",
                fieldName = "mAppContext"
            )
        )

    private const val RETAINED_VISIBLE_THRESHOLD = 5
}
