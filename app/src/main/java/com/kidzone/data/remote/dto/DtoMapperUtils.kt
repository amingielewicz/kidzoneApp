package com.kidzone.data.remote.dto

/**
 * 🎯 Odpowiedzialności:
 * - Współdzielona logika mapowania dla obiektów DTO (Firestore).
 */
object DtoMapperUtils {

    /**
     * Bezpiecznie konwertuje obiekt `Any?` (zwykle Mapę z Firestore) na `Map<String, String>`.
     * Obsługuje fallback dla starszych wersji danych i błędnych typów.
     */
    fun Any?.toPhotoHashMap(): Map<String, String> =
        (this as? Map<*, *>)
            ?.mapNotNull { (url, hash) ->
                if (url is String && hash is String) url to hash else null
            }
            ?.toMap()
            .orEmpty()
}
