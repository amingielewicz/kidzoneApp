package com.kidzone.domain.service

import android.net.Uri

/**
 * Abstrakcja potoku kompresji obrazów przed uploadem.
 *
 * Oddziela ViewModele od `ContentResolver`, `Bitmap` i szczegółów formatu wynikowego. Implementacja
 * produkcyjna deleguje do [com.kidzone.utils.ImageCompressor].
 *
 * Kompresja może być kosztowna obliczeniowo, dlatego wywołujący powinien uruchamiać ją poza głównym
 * wątkiem i nie przechowywać niepotrzebnie pełnych danych obrazu w stanie UI.
 */
interface ImageCompressorPort {

    /**
     * Odczytuje obraz spod [uri] i kompresuje go do formatu WebP.
     *
     * Implementacja powinna kontrolować rozmiar obrazu, bezpiecznie obsługiwać błędny URI oraz nie
     * logować ścieżki ani innych danych, które mogłyby ujawniać prywatne zasoby użytkownika.
     *
     * @param uri URI obrazu uzyskany z aparatu lub Android Photo Picker.
     * @return skompresowane bajty albo `null`, gdy obrazu nie można odczytać lub zdekodować.
     */
    fun compressToWebp(uri: Uri): ByteArray?
}
