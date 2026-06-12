package com.kidzone.domain.model

/**
 * Generic wrapper for cursor-based paginated results.
 *
 * @param T the type of items in the page
 * @property items the items for the current page
 * @property nextCursor opaque cursor token for fetching the next page.
 *   Null when there are no more pages (last page).
 * @property hasMore convenience flag: true when [nextCursor] != null
 */
data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean = nextCursor != null
)
