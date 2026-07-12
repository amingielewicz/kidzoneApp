package com.kidzone.utils

import android.location.Address

/**
 * Narzędzia do formatowania adresów w całej aplikacji.
 * Gwarantują spójność między listą miejsc a ekranem szczegółów.
 */
object AddressUtils {

    private const val DISPLAY_ADDRESS_PARTS_LIMIT = 3

    /**
     * Formatuje adres do postaci: Ulica Numer, Miasto, Państwo.
     * Usuwa kody pocztowe i polskie prefiksy (ul., al. itp.).
     *
     * Przykład:
     * - "ul. Targowa 6, 90-001 Łódź, Polska" -> "Targowa 6, Łódź, Polska"
     * - "Main St 123, New York, USA" -> "Main St 123, New York, USA"
     */
    fun formatDisplayAddress(address: String): String {
        if (address.isBlank()) return ""

        val postalCodeRegex = Regex("\\d{2}-\\d{3}")
        
        val parts = address.split(",")
            .map { it.replace(postalCodeRegex, "").trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, part ->
                // Usuwamy prefiksy tylko z pierwszej części (ulicy)
                if (index == 0) stripPrefixes(part) else part
            }

        // Bierzemy trzy części: Ulica Numer, Miasto, Państwo
        return parts.take(DISPLAY_ADDRESS_PARTS_LIMIT).distinct().joinToString(", ")
    }

    /**
     * Usuwa popularne polskie prefiksy adresowe.
     */
    private fun stripPrefixes(input: String): String {
        val prefixes = listOf("ul.", "ulica", "al.", "aleja", "aleje", "pl.", "plac")
        val lower = input.lowercase()
        
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                return input.substring(prefix.length).trim()
            }
        }
        return input
    }

    /**
     * Konwertuje obiekt [Address] do formatu: Ulica Numer, Miasto, Państwo.
     */
    fun fromAndroidAddress(address: Address): String? {
        val street = address.thoroughfare
        val number = address.subThoroughfare
        val city = address.locality ?: address.subAdminArea
        val country = address.countryName

        val streetPart = if (street != null) {
            if (number != null) "$street $number" else street
        } else null

        return listOfNotNull(streetPart, city, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { null }
    }
}
