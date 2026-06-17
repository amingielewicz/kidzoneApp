package com.kidzone.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.presentation.profile.NotificationPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Ładowanie i zapis preferencji powiadomień użytkownika z/do Firestore.
 *
 * Wydzielone z ProfileViewModel, żeby VM nie miał bezpośredniej
 * zależności na FirebaseFirestore (łatwiejsze testowanie, SRP).
 */
class NotificationPrefsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) {

    /**
     * Pobiera aktualne preferencje z `users/{uid}` oraz prywatną zgodę email
     * z `users/{uid}/private/profile`.
     * Zwraca domyślne wartości jeśli pole nie istnieje lub fetch padnie.
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
     * Zapisuje preferencje do Firestore (merge – nie nadpisuje reszty pól usera).
     * Fire-and-forget semantyka: zwraca true przy sukcesie, false przy błędzie.
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
