package com.playword.domain.repository

import com.playword.domain.model.User
import com.playword.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje uwierzytelniania (e-mail/hasło + Google) i obserwacja
 * aktualnie zalogowanego użytkownika.
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
}
