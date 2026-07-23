package com.kidzone.utils

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zcentralizowany komponent do generowania skrótów (hashy) treści zdjęć.
 * Używany do wykrywania duplikatów bez konieczności pobierania plików z Storage.
 */
@Singleton
class PhotoHasher @Inject constructor() {

    /**
     * Oblicza hash MD5 dla podanych bajtów (zwykle już po kompresji).
     */
    fun computeHash(bytes: ByteArray): String {
        return MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Sprawdza czy hash znajduje się w podanym zbiorze znanych hashy.
     */
    fun isDuplicate(hash: String, knownHashes: Collection<String>): Boolean {
        return hash in knownHashes
    }
}
