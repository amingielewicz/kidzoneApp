package com.kidzone.domain.repository

import com.kidzone.domain.model.User
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje uwierzytelniania (e-mail/hasło + Google), obserwacja
 * aktualnie zalogowanego użytkownika oraz odczyt publicznych danych
 * innych użytkowników (np. autora miejsca).
 */
interface AuthRepository {

    /** Strumień aktualnie zalogowanego użytkownika lub null gdy wylogowany. */
    val currentUser: Flow<User?>

    suspend fun signInWithEmail(email: String, password: String): OpResult<User>

    suspend fun registerWithEmail(name: String, email: String, password: String): OpResult<User>

    /** [idToken] pochodzi z Google Sign-In na urządzeniu. */
    suspend fun signInWithGoogle(idToken: String): OpResult<User>

    suspend fun sendPasswordResetEmail(email: String): OpResult<Unit>

    suspend fun signOut()

    /**
     * Pobiera dokument użytkownika z kolekcji `users`.
     *
     * Używane np. na ekranie szczegółów miejsca, żeby pokazać
     * "Dodano przez: {nick}". Nie wymaga, by [userId] był aktualnie
     * zalogowanym użytkownikiem.
     */
    suspend fun getUserById(userId: String): OpResult<User>

    /**
     * Top użytkowników wg [User.placesAddedCount] (sort malejąco).
     *
     * Używane przez ekran Ranking. Drugorzędne sortowanie
     * (np. po `reviewsCount`) wykonuje strona klienta, bo composite index
     * wymagałby ręcznej konfiguracji w konsoli Firebase.
     */
    suspend fun getTopUsers(limit: Int = 10): OpResult<List<User>>
}
