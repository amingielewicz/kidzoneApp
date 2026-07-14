package com.kidzone.domain.model

/**
 * Wynik pojedynczej strony danych pobieranej kursorem.
 *
 * Kursor jest nieprzezroczystym tokenem przekazywanym z powrotem do repository. Warstwa UI nie
 * powinna interpretować jego zawartości ani zakładać, że jest numerem strony.
 *
 * @param T typ elementów strony.
 * @property items elementy bieżącej strony.
 * @property nextCursor token następnej strony albo `null`, gdy bieżąca strona jest ostatnia.
 * @property hasMore wygodna flaga informująca, czy można pobrać kolejną stronę.
 */
data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean = nextCursor != null
)
