package com.kidzone.sync

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Pomocnicze rozpoznawanie błędów połączenia.
 *
 * Wynik może służyć do decyzji, czy operacja jest kandydatem do późniejszego ponowienia. Nie jest
 * jednak wystarczającą podstawą do automatycznego kolejkowania zapisu: dany typ operacji nadal musi
 * posiadać kompletny, idempotentny processor w [SyncWorker].
 */
object NetworkUtils {

    /**
     * Sprawdza, czy wyjątek wskazuje na problem transportowy lub brak łączności.
     *
     * Walidacja, brak uprawnień, konflikt danych i błąd biznesowy nie powinny zostać sklasyfikowane
     * jako błąd sieci. Dla wyjątków opakowanych sprawdzana jest również bezpośrednia przyczyna.
     *
     * @param e wyjątek zwrócony przez warstwę infrastruktury.
     * @return `true`, gdy błąd prawdopodobnie wynika z braku sieci, DNS albo timeoutu.
     */
    fun isNetworkError(e: Exception): Boolean {
        return when (e) {
            is UnknownHostException -> true
            is SocketTimeoutException -> true
            is IOException -> {
                val message = e.message?.lowercase().orEmpty()
                message.contains("network") ||
                    message.contains("unavailable") ||
                    message.contains("connection") ||
                    message.contains("timeout") ||
                    message.contains("failed to connect")
            }
            is TimeoutException -> true
            else -> {
                val cause = e.cause
                cause is IOException || cause is UnknownHostException
            }
        }
    }
}
