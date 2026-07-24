package com.kidzone.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 🎯 Odpowiedzialności:
 * - Centralizacja logiki serializacji i deserializacji danych dla encji Room.
 * - Ujednolicenie formatu przechowywania kolekcji (pipe-separated) oraz map (JSON).
 *
 * ✅ Gwarancje:
 * - Bezpieczna obsługa pustych stringów i błędnych formatów JSON (nie rzuca wyjątków).
 * - Deterministyczne mapowanie między typami prostymi bazy danych a modelami domenowymi.
 */
object LocalMapperUtils {

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    /** Konwertuje kolekcję stringów na tekst oddzielony znakiem pipe (|). */
    fun Collection<String>.toPipeSeparated(): String =
        joinToString("|")

    /** Konwertuje tekst oddzielony znakiem pipe (|) na listę stringów. */
    fun String.fromPipeSeparated(): List<String> =
        split("|").filter { it.isNotBlank() }

    /** Serializuje mapę do formatu JSON. */
    fun Map<String, String>.toJson(): String =
        gson.toJson(this)

    /** Deserializuje JSON do mapy <String, String>. */
    fun String.toPhotoHashes(): Map<String, String> {
        if (!trimStart().startsWith("{")) return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, String>>(this, mapType).orEmpty()
        }.getOrDefault(emptyMap())
    }
}
