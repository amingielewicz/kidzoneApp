package com.kidzone.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.presentation.profile.NotificationPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Ładuje i zapisuje preferencje powiadomień aktualnego użytkownika.
 *
 * Publiczne ustawienia kategorii powiadomień są przechowywane w dokumencie użytkownika, natomiast
 * prywatna zgoda na wiadomości e-mail trafia do `users/{uid}/private/profile`. Use case ukrywa
 * szczegóły Firestore przed ViewModelem i stosuje bezpieczne wartości domyślne przy braku pól.
 */
class NotificationPrefsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) {

    /**
     * Pobiera preferencje aktualnie zalogowanego użytkownika.
     *
     * Brak sesji, brak dokumentu albo błąd odczytu zwraca wartości domyślne. Metoda nie ujawnia
     * surowych wyjątków Firestore warstwie prezentacji.
     *
     * @return zapisane preferencje albo domyślny zestaw ustawień.
     */
    suspend fun load(): NotificationPrefs {
        val uid = authRepository.currentUser.first()?.id ?: return NotificationPrefs()
        return try {
            val snap = firestore
                .collection("users").document(uid).get().await()
            val privateSnap = firestore
                .collection("users").document(uid)
                .collection("private").document("profile")
                .get()
                .await()
            @Suppress("UNCHECKED_CAST")
            val prefsMap = snap.get("notificationPreferences") as? Map<String, Boolean>
            val emailEnabled = privateSnap.getBoolean("emailNotificationsEnabled")
                ?: snap.getBoolean("emailNotificationsEnabled")
                ?: true
            if (prefsMap != null) {
                NotificationPrefs(
                    newReviewOnMyPlace = prefsMap["newReviewOnMyPlace"] ?: true,
                    newBadgeEarned = prefsMap["newBadgeEarned"] ?: true,
                    newPhotoOnMyPlace = prefsMap["newPhotoOnMyPlace"] ?: true,
                    rankings = prefsMap["rankings"] ?: true,
                    emailNotificationsEnabled = emailEnabled
                )
            } else {
                NotificationPrefs(emailNotificationsEnabled = emailEnabled)
            }
        } catch (_: Exception) {
            NotificationPrefs()
        }
    }

    /**
     * Zapisuje pełny zestaw preferencji aktualnego użytkownika.
     *
     * Zapis korzysta z batcha i `merge`, dzięki czemu nie nadpisuje pozostałych pól profilu. Brak
     * sesji lub błąd któregokolwiek zapisu zwraca `false`; pełny sukces wymaga commitnięcia obu
     * dokumentów.
     *
     * @param prefs kompletny zestaw preferencji do zapisania.
     * @return `true`, gdy batch został zatwierdzony, w przeciwnym razie `false`.
     */
    suspend fun save(prefs: NotificationPrefs): Boolean {
        val uid = authRepository.currentUser.first()?.id ?: return false
        val publicData = mapOf(
            "notificationPreferences" to mapOf(
                "newReviewOnMyPlace" to prefs.newReviewOnMyPlace,
                "newBadgeEarned" to prefs.newBadgeEarned,
                "newPhotoOnMyPlace" to prefs.newPhotoOnMyPlace,
                "rankings" to prefs.rankings
            )
        )
        val privateData = mapOf(
            "userId" to uid,
            "emailNotificationsEnabled" to prefs.emailNotificationsEnabled,
            "updatedAtMillis" to System.currentTimeMillis()
        )
        return try {
            val userRef = firestore.collection("users").document(uid)
            val batch = firestore.batch()
            batch.set(userRef, publicData, SetOptions.merge())
            batch.set(
                userRef.collection("private").document("profile"),
                privateData,
                SetOptions.merge()
            )
            batch.commit().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}
