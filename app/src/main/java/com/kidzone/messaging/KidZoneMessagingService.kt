package com.kidzone.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kidzone.MainActivity
import com.kidzone.R

/**
 * Firebase Cloud Messaging service.
 *
 * Odpowiada za:
 *  - Wyświetlanie powiadomień w foreground (gdy apka jest otwarta, system
 *    nie wyświetla data message automatycznie).
 *  - Obsługę odświeżenia tokena FCM (zapisanie do Firestore).
 *
 * Powiadomienia w tle (apka zamknięta / w background) są wyświetlane
 * automatycznie przez system z pól `notification.title` / `notification.body`
 * — nie wymagają kodu po stronie klienta.
 */
class KidZoneMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Notification message (ma `notification` payload) — w foreground
        // system NIE wyświetla ich automatycznie. Musimy sami.
        val notification = message.notification
        if (notification != null) {
            showNotification(
                title = notification.title ?: "kidZone",
                body = notification.body ?: "",
                data = message.data
            )
        } else if (message.data.isNotEmpty()) {
            // Data-only message — wyświetlamy jako powiadomienie
            showNotification(
                title = message.data["title"] ?: "kidZone",
                body = message.data["body"] ?: "",
                data = message.data
            )
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = CHANNEL_GENERAL
        ensureNotificationChannel(channelId)

        // Deep link: jeśli data zawiera placeId, otworzymy szczegóły miejsca
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Powiadomienia kidZone",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Opinie, nowe miejsca i inne aktywności"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                SetOptions.merge()
            )
    }

    companion object {
        const val CHANNEL_GENERAL = "kidzone_general"

        /**
         * Zapisuje aktualny token FCM do Firestore. Wywoływane z poziomu
         * Application.onCreate() i po każdym logowaniu.
         */
        fun registerCurrentToken(context: Context) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(
                            mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                            SetOptions.merge()
                        )
                }
        }

        /**
         * Usuwa token FCM z Firestore przy wylogowaniu.
         */
        fun unregisterToken() {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmTokens", FieldValue.arrayRemove(token))
                }
        }
    }
}
